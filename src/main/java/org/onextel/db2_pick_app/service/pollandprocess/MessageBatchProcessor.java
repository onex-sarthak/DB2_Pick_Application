package org.onextel.db2_pick_app.service.pollandprocess;

import lombok.extern.slf4j.Slf4j;
import org.onextel.db2_pick_app.dto.PendingSmsDto;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MessageBatchProcessor implements Runnable {

    private final List<PendingSmsDto> batch;
    private final MessageSender messageSender;
    private final StatusUpdater statusUpdater;

    public MessageBatchProcessor(List<PendingSmsDto> batch,
                                 MessageSender messageSender,
                                 StatusUpdater statusUpdater) {
        this.batch = batch;
        this.messageSender = messageSender;
        this.statusUpdater = statusUpdater;
    }

    @Override
    public void run() {
        try {
            log.info("Processing batch of {} messages on thread {}", batch.size(), Thread.currentThread().getName());

            boolean success = messageSender.sendBatch(batch);

            if (!success) {
                log.error("CPaaS call failed. Marking batch as FAILED.");
                statusUpdater.markAsFailed(batch);
            }

        } catch (Exception e) {
            log.error("Exception during batch processing", e);
            statusUpdater.markAsFailed(batch);
        }
    }
}
