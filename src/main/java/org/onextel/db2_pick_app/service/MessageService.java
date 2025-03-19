package org.onextel.db2_pick_app.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.onextel.db2_pick_app.model.MessageInfo;
import org.onextel.db2_pick_app.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.onextel.db2_pick_app.service.interfaces.MessagePoller;
import org.onextel.db2_pick_app.service.interfaces.MessageProcessor;
import org.onextel.db2_pick_app.service.interfaces.StoredProcedureManager;
import org.onextel.db2_pick_app.service.interfaces.ThreadPoolManager;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Slf4j

public class MessageService {

    private final MessagePoller messagePoller;
    private final MessageProcessor messageProcessor;
    private final ThreadPoolManager threadPoolManager;
    private final StoredProcedureManager storedProcedureManager;
    private final MessageRepository messageRepository;

    @Value("${message.polling.interval:10}")
    private int pollingIntervalSeconds;

    @Value("${message.batch.size:100}")
    private int batchSize;

    private volatile boolean runPolling = true;

    @Autowired
    public MessageService(
            MessagePoller messagePoller,
            MessageProcessor messageProcessor,
            ThreadPoolManager threadPoolManager,
            StoredProcedureManager storedProcedureManager,
            MessageRepository messageRepository) {
        this.messagePoller = messagePoller;
        this.messageProcessor = messageProcessor;
        this.threadPoolManager = threadPoolManager;
        this.storedProcedureManager = storedProcedureManager;
        this.messageRepository = messageRepository;
    }


    @PostConstruct
    public void initialize() {
        storedProcedureManager.createFetchPendingMessagesProcedure();
        storedProcedureManager.createUpdateMessageStatusBatchProcedure();

        Executors.newSingleThreadExecutor(r -> new Thread(r, "db-poller"))
                .execute(this::continuousPollingTask);
    }

    @PreDestroy
    public void shutdown() {
        runPolling = false;
        threadPoolManager.shutdown();
    }

    private void continuousPollingTask() {
        while (runPolling) {
            try {
                boolean messagesFound = pollAndSubmitMessages();

                if (!messagesFound) {
                    Thread.sleep(pollingIntervalSeconds * 1000L);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in continuous polling task", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private boolean pollAndSubmitMessages() {
        try {
            List<MessageInfo> messages = messagePoller.fetchPendingMessages(batchSize);

            if (messages.isEmpty()) {
                return false;
            }

            threadPoolManager.submitTask(new MessageBatchProcessor(messages));
            return true;
        } catch (Exception e) {
            log.error("Error polling for messages", e);
            return false;
        }
    }

    private class MessageBatchProcessor implements Runnable {
        private final List<MessageInfo> batch;

        public MessageBatchProcessor(List<MessageInfo> batch) {
            this.batch = batch;
        }

        @Override
        public void run() {
            try {
                boolean success = messageProcessor.processMessages(batch);
                updateMessageStatus(batch, success ? 1 : 3);
            } catch (Exception e) {
                log.error("Error processing message batch", e);
                updateMessageStatus(batch, 3);
            }
        }
    }

    private void updateMessageStatus(List<MessageInfo> messages, int status) {
        try {
            String messageIds = messages.stream()
                    .map(MessageInfo::getUniqueId)
                    .collect(Collectors.joining(","));
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
}