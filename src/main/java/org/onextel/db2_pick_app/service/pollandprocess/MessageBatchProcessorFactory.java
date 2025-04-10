package org.onextel.db2_pick_app.service.pollandprocess;

import org.onextel.db2_pick_app.dto.PendingSmsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessageBatchProcessorFactory {

    private final MessageSender messageSender;
    private final StatusUpdater statusUpdater;

    @Autowired
    public MessageBatchProcessorFactory(MessageSender messageSender, StatusUpdater statusUpdater) {
        this.messageSender = messageSender;
        this.statusUpdater = statusUpdater;
    }

    public Runnable create(List<PendingSmsDto> batch) {
        return new MessageBatchProcessor(batch, messageSender, statusUpdater);
    }
}
