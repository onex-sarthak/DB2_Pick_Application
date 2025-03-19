package org.onextel.db2_pick_app.service.impl;

import org.onextel.db2_pick_app.model.MessageInfo;
import org.onextel.db2_pick_app.repository.MessageRepository;
import org.onextel.db2_pick_app.service.interfaces.MessagePoller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseMessagePoller implements MessagePoller {
    private final MessageRepository messageRepository;

    @Autowired
    public DatabaseMessagePoller(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public List<MessageInfo> fetchPendingMessages(int batchSize) {
        return messageRepository.fetchPendingMessagesBatch(batchSize);
    }
}