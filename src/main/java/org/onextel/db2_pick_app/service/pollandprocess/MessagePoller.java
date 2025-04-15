package org.onextel.db2_pick_app.service.pollandprocess;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.onextel.db2_pick_app.dto.PendingSmsDto;
import org.onextel.db2_pick_app.repository.CustomMessageRepository;
import org.onextel.db2_pick_app.service.rocksdb.RocksDBPollingHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

@Component
@Slf4j

public class MessagePoller implements Runnable {

    Gson gson = new Gson();

    @Value("${message.polling.interval:10}")
    private int pollingIntervalSeconds;

    @Value("${message.batch.size:1}")
    private int batchSize;

    private final CustomMessageRepository customMessageRepository;
    private final MessageBatchProcessorFactory processorFactory;
    private final ThreadPoolExecutor executor;
    private final StatusUpdater statusUpdater;
    private final RocksDBPollingHandler rocksDBPollingHandler;

    private volatile boolean running = true;

    public MessagePoller(CustomMessageRepository customMessageRepository,
                         MessageBatchProcessorFactory processorFactory,
                         ThreadPoolExecutor executor,
                         StatusUpdater statusUpdater,
                         RocksDBPollingHandler rocksDBPollingHandler) {
        this.customMessageRepository = customMessageRepository;
        this.processorFactory = processorFactory;
        this.executor = executor;
        this.statusUpdater = statusUpdater;
        this.rocksDBPollingHandler = rocksDBPollingHandler;
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        long lastTime = System.currentTimeMillis();

        while (running) {
            try {
                boolean foundMessages = pollAndSubmit();

                if (!foundMessages) {
                    long now = System.currentTimeMillis();
                    log.info("All messages processed. Time taken: {} seconds", (now - lastTime) / 1000);
                    lastTime = now;
                    Thread.sleep(pollingIntervalSeconds * 1000L);
                }

            } catch (InterruptedException e) {
                log.warn("Polling interrupted", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Unexpected polling error", e);
                try {
                    Thread.sleep(1000); // backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private boolean pollAndSubmit() {
        try {
            log.info("Polling for messages (batch size: {})", batchSize);
            List<PendingSmsDto> messages = customMessageRepository.fetchPendingMessagesBatch(batchSize);

            if (messages.isEmpty()) {
                log.debug("No pending messages found.");
                return false;
            }

            log.info("Submitting {} messages", messages.size());

            String id = UUID.randomUUID().toString();

            //Writing mesages to Rocks DB
            rocksDBPollingHandler.put(id , gson.toJson(messages));

            //Update status to 1 in db2 for all messages
            statusUpdater.markAsSucceeded(messages);

            Runnable task = processorFactory.create(messages,id);
            executor.submit(task);

            return true;
        } catch (RejectedExecutionException e) {
            log.warn("Task rejected by thread pool", e);
            return false;
        } catch (Exception e) {
            log.error("Error during polling", e);
            return false;
        }
    }
}
