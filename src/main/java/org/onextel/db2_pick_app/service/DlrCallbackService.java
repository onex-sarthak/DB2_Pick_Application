package org.onextel.db2_pick_app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onextel.db2_pick_app.dto.DlrCallbackRequestDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DlrCallbackService {

    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 5;
    private static final int TIMEOUT_SECONDS = 60;

    private final List<DlrCallbackRequestDto> batchBuffer = Collections.synchronizedList(new ArrayList<>());

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledFlush;

    public DlrCallbackService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Called from controller when DLR callback is received
     */
    public void addToBatch(DlrCallbackRequestDto request) {
        synchronized (batchBuffer) {
            batchBuffer.add(request);

            if (batchBuffer.size() >= BATCH_SIZE) {
                log.info("Batch buffer is full, flushing batch");
                flushBatch(); // Immediate flush
            } else if (batchBuffer.size() == 1) {
                // First entry in an empty batch — start/reset timer
                scheduleNextFlush();
            }
        }
    }

    /**
     * Resets the timer to flush after TIMEOUT_SECONDS
     */
    private void scheduleNextFlush() {
        if (scheduledFlush != null && !scheduledFlush.isCancelled()) {
            scheduledFlush.cancel(false);
        }

        scheduledFlush = scheduler.schedule(this::flushBatchDueToTimer, TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private void flushBatchDueToTimer() {
        log.info("Flush triggered by timer after {} seconds", TIMEOUT_SECONDS);
        flushBatch();
    }

    private void flushBatch() {
        List<DlrCallbackRequestDto> toProcess;
        synchronized (batchBuffer) {
            if (batchBuffer.isEmpty()) return;

            toProcess = new ArrayList<>(batchBuffer);
            batchBuffer.clear();
        }

        try {
            processBatch(toProcess);
        } catch (Exception e) {
            log.error("Batch processing failed, attempting fault isolation...", e);
            splitAndRetryBatch(toProcess);
        }

        // Reset the flush timer
        scheduleNextFlush();
    }

    private void processBatch(List<DlrCallbackRequestDto> batch) throws SQLException {
        String sql = "UPDATE SMS.SMS_TEMP_OUT_LOG_DLR " +
                "SET DELIVERY_STATUS = ?, DELIVERY_CODE = ?, DELIVERY_TIME = ? " +
                "WHERE SR_NO = ?";

        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            connection.setAutoCommit(false);

            for (DlrCallbackRequestDto request : batch) {
                preparedStatement.setString(1, request.getDlrStatus());
                preparedStatement.setString(2, request.getDeliveryCode());
                preparedStatement.setTimestamp(3, Timestamp.valueOf(request.getDeliveryTime()));
                preparedStatement.setLong(4, request.getSrNo());
                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();
            connection.commit();
            log.info("Successfully committed batch of size: {}", batch.size());
        }
    }

    private void splitAndRetryBatch(List<DlrCallbackRequestDto> batch) {
        if (batch.size() == 1) {
            log.error("Faulty request: {}", batch.getFirst());
            return;
        }

        int mid = batch.size() / 2;
        List<DlrCallbackRequestDto> firstHalf = batch.subList(0, mid);
        List<DlrCallbackRequestDto> secondHalf = batch.subList(mid, batch.size());

        try {
            processBatch(firstHalf);
        } catch (SQLException e) {
            splitAndRetryBatch(new ArrayList<>(firstHalf));
        }

        try {
            processBatch(secondHalf);
        } catch (SQLException e) {
            splitAndRetryBatch(new ArrayList<>(secondHalf));
        }
    }
}