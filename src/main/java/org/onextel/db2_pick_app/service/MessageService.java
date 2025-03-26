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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class MessageService {

    // Configuration
    @Value("${thread.pool.core.size:3}")
    private int corePoolSize;

    @Value("${thread.pool.max.size:5}")
    private int maxPoolSize;

    @Value("${thread.pool.keep.alive.seconds:60}")
    private int keepAliveSeconds;

    @Value("${message.polling.interval:10}")
    private int pollingIntervalSeconds;

    @Value("${message.batch.size:1}")
    private int batchSize;

    @Value("${thread.pool.queue.capacity:100}")
    private int queueCapacity;

    private final MessageRepository messageRepository;
    private final CPaaSIntegrationService cPaaSIntegrationService;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;


    // Thread pools
    private ExecutorService messagePollingExecutor;
    private ThreadPoolExecutor messageProcessorExecutor;

    // Flags for coordination
    private volatile boolean runPolling = true;
    private final AtomicInteger threadCounter = new AtomicInteger(0);

    @Autowired
    public MessageService(
            MessageRepository messageRepository,
            CPaaSIntegrationService cPaaSIntegrationService,
            JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.messageRepository = messageRepository;
        this.cPaaSIntegrationService = cPaaSIntegrationService;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Main polling method that is executed as a continuous task
     */
    private void continuousPollingTask() {
        while (runPolling) {
            try {
                    boolean messagesFound = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                                try {
                                    return pollAndSubmitMessages();
                                } catch (Exception e) {
                                    status.setRollbackOnly();
                                    throw e;
                                }
                            }
                    ));

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
     * to the ThreadPoolExecutor for processing
     * @return true if messages were found and submitted, false otherwise
     */

    protected boolean pollAndSubmitMessages() {
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

            // Submit the batch to the thread pool executor
            messageProcessorExecutor.submit(new MessageBatchProcessor(messages));

            log.info("Submitted batch of {} messages to the thread pool (current queue size: {})",
                    messages.size(), messageProcessorExecutor.getQueue().size());

            return true;
        } catch (RejectedExecutionException e) {
            log.warn("Thread pool rejected the task, queue might be full", e);
            return false;
        } catch (Exception e) {
            log.error("Error polling for messages", e);
            return false;
        }
    }


    //  Runnable class to process a batch of messages

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
                if (response) {
                    updateMessageStatus(batch, 1); // 1 = PickedUp
                } else {
                    updateMessageStatus(batch, 3); // 3 = Failed
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


    //  Updates the status of a batch of messages
    private void updateMessageStatus(List<MessageInfo> messages, int status) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        try {
            String messageIds = messages.stream()
                    .map(MessageInfo::getUniqueId)
                    .collect(Collectors.joining(","));
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


    @PreDestroy
    public void shutdown() {
        log.info("Shutting down message service...");

        // Signal polling thread to stop
        runPolling = false;

        if (messagePollingExecutor != null) {
            messagePollingExecutor.shutdown();
            try {
                if (!messagePollingExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    messagePollingExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                messagePollingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (messageProcessorExecutor != null) {
            messageProcessorExecutor.shutdown();
            try {
                if (!messageProcessorExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    messageProcessorExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                messageProcessorExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("Message service shutdown complete. Queue size at shutdown: {}",
                messageProcessorExecutor != null ? messageProcessorExecutor.getQueue().size() : 0);
    }

    @PostConstruct
    public void initialize() {
        // Initialize thread pools
        messagePollingExecutor = Executors.newSingleThreadExecutor(
                r -> {
                    Thread t = new Thread(r, "db-poller");
                    return t;
                });

        // Initialize message processor thread pool with LinkedBlockingQueue
        messageProcessorExecutor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                r -> {
                    Thread t = new Thread(r, "message-processor-" + threadCounter.getAndIncrement());
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // If queue is full, the submitting thread runs the task
        );

        // Start continuous polling task
        messagePollingExecutor.execute(this::continuousPollingTask);

        log.info("Message service initialized with core pool size: {}, max pool size: {}, " +
                        "queue capacity: {}, and polling interval: {} seconds",
                corePoolSize, maxPoolSize, queueCapacity, pollingIntervalSeconds);
    }
}