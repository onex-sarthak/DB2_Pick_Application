package org.onextel.db2_pick_app.service.pollandprocess;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.onextel.db2_pick_app.repository.CustomMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Service
@Slf4j
public class MessageService {

    private final CustomMessageRepository customMessageRepository;
    private final CPaaSIntegrationService cPaaSIntegrationService;
    private final JdbcTemplate jdbcTemplate;
    private final StatusUpdater statusUpdater;
    private final MessageSender messageSender;
    private final MessagePoller messagePoller;
    private final ThreadPoolExecutor messageProcessorExecutor;
    private final ExecutorService messagePollingExecutor;

    @Autowired
    public MessageService(
            CustomMessageRepository customMessageRepository,
            CPaaSIntegrationService cPaaSIntegrationService,
            JdbcTemplate jdbcTemplate,
            StatusUpdater statusUpdater,
            MessageSender messageSender,
            MessagePoller messagePoller,
            @Qualifier("messageProcessorExecutor") ThreadPoolExecutor messageProcessorExecutor,
            @Qualifier("messagePollingExecutor") ExecutorService messagePollingExecutor
    ) {
        this.customMessageRepository = customMessageRepository;
        this.cPaaSIntegrationService = cPaaSIntegrationService;
        this.jdbcTemplate = jdbcTemplate;
        this.statusUpdater = statusUpdater;
        this.messageSender = messageSender;
        this.messagePoller = messagePoller;
        this.messageProcessorExecutor = messageProcessorExecutor;
        this.messagePollingExecutor = messagePollingExecutor;
    }

    @PostConstruct
    public void startPolling() {
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
}
