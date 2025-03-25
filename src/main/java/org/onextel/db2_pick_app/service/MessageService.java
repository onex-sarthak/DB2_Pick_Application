package org.onextel.db2_pick_app.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.onextel.db2_pick_app.model.MessageInfo;
import org.onextel.db2_pick_app.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@AllArgsConstructor
@Slf4j
public class MessageService {

    // Configuration
    AtomicInteger activeVirtualThreads = new AtomicInteger(0);
    @Value("${message.polling.interval:10}")
    private int pollingIntervalSeconds;

    @Value("${message.batch.size:1}")
    private int batchSize;

    private final MessageRepository messageRepository;
    private final CPaaSIntegrationService cPaaSIntegrationService;
    private final JdbcTemplate jdbcTemplate;

    // Executor for virtual threads
    private ExecutorService virtualThreadExecutor;

    // Flags for coordination
    private volatile boolean runPolling = true;

    @Autowired
    public MessageService(
            MessageRepository messageRepository,
            CPaaSIntegrationService cPaaSIntegrationService,
            JdbcTemplate jdbcTemplate) {
        this.messageRepository = messageRepository;
        this.cPaaSIntegrationService = cPaaSIntegrationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Main polling method that is executed as a continuous task
     */
    private void continuousPollingTask() {
        while (runPolling) {
            try {
                boolean messagesFound = pollAndSubmitMessages();

                // If no messages were found, sleep for the polling interval
                if (!messagesFound) {
                    log.debug("No messages found, sleeping for {} seconds", pollingIntervalSeconds);
                    Thread.sleep(pollingIntervalSeconds * 1000L);
                }
                // If messages were found, continue immediately with the next poll
            } catch (InterruptedException e) {
                log.warn("Polling thread interrupted", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in continuous polling task", e);
                try {
                    // Sleep briefly to avoid tight error loops
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Polls the database for pending messages and submits them
     * to the virtual thread executor for processing
     * @return true if messages were found and submitted, false otherwise
     */
    private boolean pollAndSubmitMessages() {
        try {
            log.info("Polling for pending messages (batch size: {})...", batchSize);

            // Use repository to fetch pending messages
            List<MessageInfo> messages = messageRepository.fetchPendingMessagesBatch(batchSize);

            if (messages.isEmpty()) {
                log.info("No pending messages found");
                return false;
            }

            log.info("Found {} pending messages, submitting for processing", messages.size());

            // Update status to prevent other threads from picking these messages
            updateMessageStatus(messages, 1); // 1 = Processing

            // Submit the batch to the virtual thread executor

//            for (Thread thread : Thread.getAllStackTraces().keySet()) {
//                if (thread.isVirtual()) {
//                    activeVirtualThreads.getAndIncrement();
//                }
//            }
            System.out.println("Active Threads: " + Thread.activeCount());
            virtualThreadExecutor.submit(new MessageBatchProcessor(messages));

            log.info("Submitted batch of {} messages to the virtual thread executor", messages.size());

            return true;
        } catch (RejectedExecutionException e) {
            log.warn("Virtual thread executor rejected the task", e);
            return false;
        } catch (Exception e) {
            log.error("Error polling for messages", e);
            return false;
        }
    }

    /**
     * Runnable class to process a batch of messages
     */
    private class MessageBatchProcessor implements Runnable {
        private final List<MessageInfo> batch;

        public MessageBatchProcessor(List<MessageInfo> batch) {
            this.batch = batch;
        }

        @Override
        public void run() {
            try {
                log.info("Processing batch of {} messages by thread {}", batch.size(), Thread.currentThread().getName());

                // Process the batch using the CPaaS integration service
                boolean response = cPaaSIntegrationService.sendMessagesInBatch(batch);

                // Update message status based on result
                if (!response) {
                    updateMessageStatus(batch, 3); // 3 = failed
                }
            } catch (Exception e) {
                log.error("Error processing message batch", e);

                // Update status of failed batch
                try {
                    updateMessageStatus(batch, 3); // 3 = Failed
                } catch (Exception ex) {
                    log.error("Failed to update status for failed messages", ex);
                }
            }
        }
    }

    // Other methods (updateMessageStatus, resetMessageStatus, createStoredProcedures, etc.) remain unchanged

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down message service...");

        // Signal polling thread to stop
        runPolling = false;

        if (virtualThreadExecutor != null) {
            virtualThreadExecutor.shutdown();
            try {
                if (!virtualThreadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    virtualThreadExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                virtualThreadExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("Message service shutdown complete.");
    }

    @PostConstruct
    public void initialize() {
        // Create stored procedures
        createStoredProcedures();

        // Initialize virtual thread executor
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        // Start continuous polling task on a virtual thread
        virtualThreadExecutor.execute(this::continuousPollingTask);

        log.info("Message service initialized with virtual threads and polling interval: {} seconds", pollingIntervalSeconds);
    }
    //  Updates the status of a batch of messages
    private void updateMessageStatus(List<MessageInfo> messages, int status) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        try {
            String messageIds = messages.stream()
                    .map(MessageInfo::getUniqueId)
                    .collect(java.util.stream.Collectors.joining(","));
            log.info("Updating status to {} for {} ", status, messageIds);
            messageRepository.updateMessageStatusBatch(messageIds, status);
        } catch (Exception e) {
            log.error("Error updating message status", e);
        }
    }


    //  Method to reset message status (for error recovery or testing)
    @Transactional
    public void resetMessageStatus(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        messageRepository.updateMessageStatusBatch(String.join(",", ids), 0);
        log.info("Reset status for {} messages", ids.size());
    }

    private void createFetchPendingMessagesProcedure() {
        String sql = """  
                CREATE OR REPLACE PROCEDURE SMS_SCHEMA.FETCH_PENDING_MESSAGES(
                            IN BATCH_SIZE INTEGER
                        )
                        LANGUAGE SQL
                        SPECIFIC FETCH_PENDING_MESSAGES
                        RESULT SETS 1
                        BEGIN
                            -- Declare cursor
                            DECLARE message_cursor CURSOR WITH RETURN FOR
                                SELECT *
                                FROM SMS_SCHEMA.MESSAGE_INFO
                                WHERE STATUS_FLAG = 0
                                ORDER BY TIMESTAMP ASC
                                FETCH FIRST BATCH_SIZE ROWS ONLY;

                            -- Set lock wait timeout to prevent deadlocks
                            SET CURRENT LOCK TIMEOUT 10;

                            -- Open the cursor
                            OPEN message_cursor;
                        END       
        """;
        jdbcTemplate.execute(sql);
        log.warn("Stored procedure FETCH_PENDING_MESSAGES created");
    }

    private void createUpdateMessageStatusBatchProcedure() {
        String sql = """  
                CREATE OR REPLACE PROCEDURE SMS_SCHEMA.UPDATE_MESSAGE_STATUS_BATCH(
                                                                       IN UNIQUE_IDS VARCHAR(1000),  -- Comma-separated list of UNIQUE_ID values
                                                                       IN NEW_STATUS INTEGER         -- New status value to set
                                                                   )
                                                                   LANGUAGE SQL
                                                                   BEGIN
                                                                       DECLARE SQL_QUERY VARCHAR(2000);
                                                                       DECLARE v_unique_ids VARCHAR(1000);

                                                                       -- Wrap each UNIQUE_ID in single quotes and separate with commas
                                                                       SET v_unique_ids = REPLACE(TRIM(UNIQUE_IDS), ',', ''',''');

                                                                       -- Add single quotes at the beginning and end of the list
                                                                       SET v_unique_ids = '''' || v_unique_ids || '''';
  
                                                                       -- Build the SQL query dynamically
                                                                       SET SQL_QUERY = 'UPDATE SMS_SCHEMA.MESSAGE_INFO SET STATUS_FLAG = ' || NEW_STATUS ||
                                                                                       ' WHERE UNIQUE_ID IN (' || v_unique_ids || ')';

                                                                       -- Execute the dynamically constructed SQL query
                                                                       EXECUTE IMMEDIATE SQL_QUERY;

                                                                       -- Commit the changes
                                                                       COMMIT;
                                                                   END;
        """;
        jdbcTemplate.execute(sql);
        log.warn("Stored procedure UPDATE_MESSAGE_STATUS_BATCH created");
    }

    private void createStoredProcedures() {
        createFetchPendingMessagesProcedure();
        createUpdateMessageStatusBatchProcedure();
    }
}