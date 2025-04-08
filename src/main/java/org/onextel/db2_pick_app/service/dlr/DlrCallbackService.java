package org.onextel.db2_pick_app.service.dlr;

import lombok.extern.slf4j.Slf4j;
import org.onextel.db2_pick_app.dto.DlrCallbackRequestDto;
import org.onextel.db2_pick_app.repository.DlrCallbackRepository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class DlrCallbackService {

    private static final int BATCH_SIZE = 1000;
    private static final long TIMEOUT_MILLISECONDS = 50;

    private final DlrCallbackBuffer buffer;
    private final DlrBatchScheduler scheduler;
    private final DlrCallbackRepository repository;
    private final Executor flushexecutor;


    public DlrCallbackService(
            DlrCallbackBuffer buffer,
            DlrBatchScheduler scheduler,
            DlrCallbackRepository repository,
            @Qualifier("dlrCallbackExecutor") Executor flushexecutor // 👈 Qualifier used here
    ) {
        this.buffer = buffer;
        this.scheduler = scheduler;
        this.repository = repository;
        this.flushexecutor = flushexecutor;
    }

    public void addToBatch(DlrCallbackRequestDto request) {
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
        } catch (Exception e) {
            log.error("Failed to flush DLR batch", e);
        }

        if (!buffer.isEmpty()) {
            scheduler.schedule(this::flushBatch, TIMEOUT_MILLISECONDS);
        }
    }
}
