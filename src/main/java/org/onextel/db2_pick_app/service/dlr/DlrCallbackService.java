package org.onextel.db2_pick_app.service.dlr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.extern.slf4j.Slf4j;
import org.onextel.db2_pick_app.dto.DlrCallbackRequestDto;
import org.onextel.db2_pick_app.repository.DlrCallbackRepository;

import org.onextel.db2_pick_app.service.rocksdb.RocksDBDlrHandler;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class DlrCallbackService {

    // TODO: Declare these variables in application.yaml and use them here through @Value annotatoin
    private static final int BATCH_SIZE = 1000;
    private static final long TIMEOUT_MILLISECONDS = 100000;

    private final DlrCallbackBuffer buffer;
    private final DlrBatchScheduler scheduler;
    private final DlrCallbackRepository repository;
    private final Executor flushexecutor;
    private final RocksDBDlrHandler rocksDBDlrHandler;
    private final ObjectMapper objectMapper;

    public DlrCallbackService(
            DlrCallbackBuffer buffer,
            DlrBatchScheduler scheduler,
            DlrCallbackRepository repository,
            @Qualifier("dlrCallbackExecutor") Executor flushexecutor, // Qualifier used here
            RocksDBDlrHandler rocksDBDlrHandler,
            ObjectMapper objectMapper
    ) {
        this.buffer = buffer;
        this.scheduler = scheduler;
        this.repository = repository;
        this.flushexecutor = flushexecutor;
        this.rocksDBDlrHandler = rocksDBDlrHandler;
        this.objectMapper = objectMapper;
    }

    public void addToBatch(DlrCallbackRequestDto request){

        try{
            String json = objectMapper.writeValueAsString(request);
            rocksDBDlrHandler.put(request.getSrNo().toString(), json);
            log.info("Adding DlrCallbackRequestDto to DB: " + json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        boolean shouldFlush = buffer.add(request, BATCH_SIZE);
        if (shouldFlush) {
            log.info("Batch full, flushing");
            scheduler.cancel();
            flushexecutor.execute(this::flushBatch);
        } else if (buffer.size() == 1) {
            scheduler.schedule(this::flushBatch, TIMEOUT_MILLISECONDS);
        }
    }

    private void flushBatch() {
        List<DlrCallbackRequestDto> toProcess = buffer.drain();
        if (toProcess.isEmpty()) return;

        try {
            repository.updateBatch(toProcess);
            log.info("Flushed {} DLR records", toProcess.size());

            //  Delete from RocksDB
            toProcess.forEach(req -> {
                String key = String.valueOf(req.getSrNo());
                rocksDBDlrHandler.delete(key);
            });

        } catch (Exception e) {
            log.error("Failed to flush DLR batch", e);
        }

        if (!buffer.isEmpty()) {
            scheduler.schedule(this::flushBatch, TIMEOUT_MILLISECONDS);
        }
    }

    public void processRocksDBDlrData(){
        List<String> keys = rocksDBDlrHandler.iterate();
        for (String key : keys) {
            try{
                String serializedMessages = rocksDBDlrHandler.get(key);
                if (serializedMessages != null) {
                    DlrCallbackRequestDto dlr = objectMapper.readValue(serializedMessages, DlrCallbackRequestDto.class);
                    log.info("Submitting recovered batch from RocksDB with key: {} and value: {}", key,dlr);
                    addToBatch(dlr);
                }

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
