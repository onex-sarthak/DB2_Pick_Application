package com.onextel.db2_pick_app.service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import com.onextel.db2_pick_app.model.MessageInfo;
import com.onextel.db2_pick_app.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@AllArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final CPaaSIntegrationService cPaaSIntegrationService;
    private final JdbcTemplate jdbcTemplate;

    // Thread pools
    private ScheduledExecutorService producerExecutor;
    private ExecutorService consumerExecutor;

    // Message queue
    private final BlockingQueue<List<MessageInfo>> messageQueue = new LinkedBlockingQueue<>();

    // Semaphore to control consumer thread availability
    private final Semaphore consumerSemaphore;

    // Flags for coordination
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private volatile boolean noMessagesFound = false;
    private volatile long lastEmptyPollTime = 0;

    // Configuration
    @Value("${consumer.thread.pool.size:5}")
    private int consumerThreadPoolSize;

    @Value("${message.polling.interval:120}")
    private int pollingIntervalSeconds;

    @Value("${message.batch.size:100}")
    private int batchSize;

    @Value("${api.call.batch.size:20}")
    private int apiCallBatchSize;

    @Autowired
    public MessageService(
            MessageRepository messageRepository,
            CPaaSIntegrationService cPaaSIntegrationService,
            JdbcTemplate jdbcTemplate) {
        this.messageRepository = messageRepository;
        this.cPaaSIntegrationService = cPaaSIntegrationService;
        this.jdbcTemplate = jdbcTemplate;
        this.consumerSemaphore = new Semaphore(consumerThreadPoolSize); // Initially no permits
    }

    @PostConstruct
    public void initialize() {
        // Create stored procedures
        createStoredProcedures();

        // Initialize thread pools
        producerExecutor = Executors.newScheduledThreadPool(1,
                r -> {
                    Thread t = new Thread(r, "db-poller");
//                    t.setDaemon(true);
                    return t;
                });

        consumerExecutor = Executors.newFixedThreadPool(consumerThreadPoolSize,
                r -> {
                    Thread t = new Thread(r, "api-caller");
//                    t.setDaemon(true);
                    return t;
                });

        // Release all permits to the semaphore initially
        consumerSemaphore.release(consumerThreadPoolSize);

        // Start producer thread with fixed rate scheduling
        producerExecutor.scheduleAtFixedRate(
                this::pollAndQueueMessages,
                0,
                pollingIntervalSeconds,
                TimeUnit.SECONDS);

        // Start consumer threads
        for (int i = 0; i < consumerThreadPoolSize; i++) {
            final int consumerId = i;
            consumerExecutor.submit(() -> processMessages(consumerId));
        }

        log.info("Message service initialized with {} consumer threads and polling interval of {} seconds",
                consumerThreadPoolSize, pollingIntervalSeconds);
    }

    private void createStoredProcedures() {
        createFetchPendingMessagesProcedure();
        createUpdateMessageStatusBatchProcedure();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down message service...");

        if (producerExecutor != null) {
            producerExecutor.shutdown();
            try {
                if (!producerExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    producerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                producerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (consumerExecutor != null) {
            consumerExecutor.shutdown();
            try {
                if (!consumerExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    consumerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                consumerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("Message service shutdown complete");
    }

    /**
     * Producer method: Polls the database for pending messages and adds them to the queue
     */
    private void pollAndQueueMessages() {
        try {
            // Check if we should skip polling due to recent empty poll
            long currentTime = System.currentTimeMillis();
            if (noMessagesFound && (currentTime - lastEmptyPollTime) < pollingIntervalSeconds * 1000L) {
                log.info("Skipping poll - no messages found in recent poll within last {} seconds",
                        pollingIntervalSeconds);
                return;
            }

            // Before polling, check if any consumer threads are available
            log.info("Waiting for available consumer thread before polling...");
            if (!consumerSemaphore.tryAcquire(1, 5, TimeUnit.SECONDS)) {
                log.info("No consumer threads available, skipping this poll cycle");
                return;
            }

            try {
                log.info("Polling for pending messages (batch size: {})...", batchSize);

                // Use repository to fetch pending messages
                List<MessageInfo> messages = messageRepository.fetchPendingMessagesBatch(batchSize);

                if (messages.isEmpty()) {
                    log.info("No pending messages found");
                    noMessagesFound = true;
                    lastEmptyPollTime = System.currentTimeMillis();
                    return;
                }

                log.info("Found {} pending messages, queueing for processing", messages.size());

                // Update status to prevent other threads from picking these messages
                updateMessageStatus(messages, 1); // 1 = Processing

                // Add messages to the queue
                messageQueue.put(messages);

                // Reset flag since we found messages
                noMessagesFound = false;

            } finally {
                // Release the semaphore permit we acquired
                consumerSemaphore.release();
                log.info("Semaphore released by producer, available permits: {}", consumerSemaphore.availablePermits());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Producer thread interrupted while waiting for semaphore");
        } catch (Exception e) {
            log.error("Error polling for messages", e);
        }
    }

    /**
     * Consumer method: Processes messages from the queue
     */
    private void processMessages(int consumerId) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Try to acquire a permit from the semaphore
                consumerSemaphore.acquire();

                try {
                    // Try to fill a batch from the queue
                    List<MessageInfo> batch = messageQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (batch == null) {
                        // No messages in queue, wait for polling interval if queue is empty
                        log.debug("Consumer {} found no messages, waiting...", consumerId);
                        Thread.sleep(pollingIntervalSeconds * 1000);
                        continue;
                    }


                    // Process the batch
                    log.info("Consumer {} processing batch of {} messages", consumerId, batch.size());
                    cPaaSIntegrationService.sendMessagesInBatch(batch);

                } finally {
                    // Always release the permit
                    consumerSemaphore.release();
                    log.info("Semaphore released by consumer {}, available permits: {}", consumerId, consumerSemaphore.availablePermits());
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Consumer {} interrupted", consumerId);
                break;
            } catch (Exception e) {
                log.error("Error processing messages in consumer {}", consumerId, e);

                // Short delay to prevent CPU spinning on errors
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Updates the status of a batch of messages
     */
    private void updateMessageStatus(List<MessageInfo> messages, int status) {
        try {
            String messageIds = messages.stream()
                    .map(MessageInfo::getUniqueId)
                    .collect(java.util.stream.Collectors.joining(","));
            log.info("Updating message status for {} messages", messageIds);
            messageRepository.updateMessageStatusBatch(messageIds, status);
        } catch (Exception e) {
            log.error("Error updating message status", e);
        }
    }

    /**
     * Method to reset message status (for error recovery or testing)
     */
    @Transactional
    public void updateStatusFlagsToZero(List<String> ids) {
        messageRepository.updateMessageStatusBatch(String.join(",", ids), 0);
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
        log.warn("Stored procedure FETCH_PENDING_MESSAGES");
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
                                                               SET SQL_QUERY = 'UPDATE SMS_SCHEMA.MESSAGE_INFO SET STATUS_FLAG = ' || NEW_STATUS ||\s
                                                                               ' WHERE UNIQUE_ID IN (' || v_unique_ids || ')';
                                                           
                                                               -- Execute the dynamically constructed SQL query
                                                               EXECUTE IMMEDIATE SQL_QUERY;
                                                           
                                                               -- Commit the changes
                                                               COMMIT;
                                                           END;
        """;
        jdbcTemplate.execute(sql);
        log.warn("Stored procedure createUpdateMessageStatusBatchProcedure");

    }

}