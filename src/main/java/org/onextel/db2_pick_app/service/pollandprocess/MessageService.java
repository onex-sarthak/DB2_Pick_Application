package org.onextel.db2_pick_app.service.pollandprocess;

import com.google.gson.reflect.TypeToken;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.Type;

import org.onextel.db2_pick_app.dto.DlrCallbackRequestDto;
import org.onextel.db2_pick_app.dto.PendingSmsDto;
import org.onextel.db2_pick_app.repository.CustomMessageRepository;
import org.onextel.db2_pick_app.service.dlr.DlrCallbackService;
import org.onextel.db2_pick_app.service.rocksdb.RocksDBPollingHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Service
@Slf4j
public class MessageService {

    private final StatusUpdater statusUpdater;
    private final MessagePoller messagePoller;
    private final ThreadPoolExecutor messageProcessorExecutor;
    private final ExecutorService messagePollingExecutor;
    private final RocksDBPollingHandler rocksDBPollingHandler;
    private final MessageBatchProcessorFactory processorFactory;
    private final DlrCallbackService dlrCallbackService;


    @Autowired
    public MessageService(
            StatusUpdater statusUpdater,
            MessagePoller messagePoller,
            @Qualifier("messageProcessorExecutor") ThreadPoolExecutor messageProcessorExecutor,
            @Qualifier("messagePollingExecutor") ExecutorService messagePollingExecutor,
            RocksDBPollingHandler rocksDBPollingHandler,
            MessageBatchProcessorFactory processorFactory,
            DlrCallbackService dlrCallbackService
    ) {
        this.statusUpdater = statusUpdater;
        this.messagePoller = messagePoller;
        this.messageProcessorExecutor = messageProcessorExecutor;
        this.messagePollingExecutor = messagePollingExecutor;
        this.rocksDBPollingHandler = rocksDBPollingHandler;
        this.processorFactory = processorFactory;
        this.dlrCallbackService = dlrCallbackService;
    }

    @PostConstruct
    public void startPolling() {
        log.info("Processing messages present in rocksdb-polling-data");
        processRocksDBData();

        log.info("Processing messages present in rocksdb-dlr-data");
        dlrCallbackService.processRocksDBDlrData();

        log.info("Starting message polling...");
        messagePollingExecutor.execute(messagePoller);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down message service...");
        messagePoller.stop();

        if (messagePollingExecutor != null) {
            messagePollingExecutor.shutdown();
            try {
                if (!messagePollingExecutor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    messagePollingExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                messagePollingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (messageProcessorExecutor != null) {
            messageProcessorExecutor.shutdown();
        }

        log.info("Message service shutdown complete.");
    }

    // Public admin/testing method
    public void resetMessageStatusToZero(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;

        statusUpdater.resetToPending(ids);
        log.info("Reset status for {} messages", ids.size());
    }

    public void processRocksDBData(){
        // Iterate over RocksDB data
        List<String> keys = rocksDBPollingHandler.iterate();
        Gson gson = new Gson();
        Type listOfPendingSmsDto = new TypeToken<List<PendingSmsDto>>() {}.getType();

        for (String key : keys) {
            try {
                String serializedMessages = rocksDBPollingHandler.get(key);
                if (serializedMessages != null) {
                    // Deserialization of messages
                    List<PendingSmsDto> messages = gson.fromJson(serializedMessages,
                            listOfPendingSmsDto);

                    log.info("Submitting recovered batch from RocksDB with key: {} and value: {}", key,messages);

                    // Submit the recovered batch
                    Runnable task = processorFactory.create(messages, key);
                    messageProcessorExecutor.submit(task);

                }
            } catch (Exception e) {
                log.error("Error processing RocksDB entry with key: {}", key, e);
            }
        }
    }
}
