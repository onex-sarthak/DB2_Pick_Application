package org.example.db2_pick_app.service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import org.example.db2_pick_app.model.MessageInfo;
import org.example.db2_pick_app.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private CPaaSIntegrationService cPaaSIntegrationService;

    private ScheduledThreadPoolExecutor scheduler;

    // Track when no messages were found
    private volatile boolean noMessagesFound = false;
    private volatile long lastEmptyPollTime = 0;

    @Value("${thread.pool.core.size:2}")
    private int threadPoolCoreSize;

    @Value("${message.polling.interval:120}")
    private int pollingIntervalSeconds;

    @Value("${message.batch.size:2}")
    private int batchSize;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MessageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
        logger.warn("Stored procedure FETCH_PENDING_MESSAGES");
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
        logger.warn("Stored procedure createUpdateMessageStatusBatchProcedure");

    }

    @PostConstruct
    public void startScheduler() {
        createFetchPendingMessagesProcedure();
        createUpdateMessageStatusBatchProcedure();
        scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(threadPoolCoreSize);

        for (int i = 0; i < threadPoolCoreSize; i++) {
            final int theadId = i;
            long initialDelay = i * (pollingIntervalSeconds / threadPoolCoreSize);
            scheduler.scheduleAtFixedRate(() -> {
                logger.info("Thread {} polling for messages", theadId);
                coordinatedPollAndProcessMessages(theadId);
            }, initialDelay, pollingIntervalSeconds, TimeUnit.SECONDS);
        }
        logger.info("Message processing scheduler started with {} threads, polling every {} seconds",
                threadPoolCoreSize, pollingIntervalSeconds);
    }

    @PreDestroy
    public void stopScheduler() {
        if (scheduler != null) {
            logger.info("Shutting down message processing scheduler");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    @Transactional
    public void updateStatusFlagsToZero(List<String> ids) {
        messageRepository.updateMessageStatusBatch(String.join(",", ids),0);
    }

    public void coordinatedPollAndProcessMessages(int threadId) {

        try{
            long currentTime = System.currentTimeMillis();
            if(noMessagesFound && (currentTime - lastEmptyPollTime) < pollingIntervalSeconds * 1000) {
                logger.info("Thread {} skipping poll - no messages found in recent poll within last {} seconds",
                        threadId, pollingIntervalSeconds);
                return;
            }
            logger.info("Thread {} checking for messages", threadId);
            pollAndProcessMessages();
        }catch(Exception e) {
            logger.error("Error occurred while polling for messages", e);
        }
    }

    @Transactional
    public void pollAndProcessMessages() {

        try {
            logger.info("Polling for new messages (batch size: {})...", batchSize);

            // Use repository to call stored procedure for fetching messages
            List<MessageInfo> messages = messageRepository.fetchPendingMessagesBatch(batchSize);

            if (messages.isEmpty()) {
                logger.info("No pending messages found");
                noMessagesFound = true;
                lastEmptyPollTime = System.currentTimeMillis();
                return;
            }
            // Reset the flag since we found messages
            noMessagesFound = false;

            logger.info("Processing {} messages...", messages.size());

            // Process each message
            try {
                String response = cPaaSIntegrationService.sendMessagesInBatch(messages);

                logger.debug("Successfully processed messages: {}", response);
            } catch (Exception e) {
                logger.error("Failed to process messages", e);
            }

        } catch (Exception e) {
            logger.error("Error during message batch processing", e);
        }
    }
}