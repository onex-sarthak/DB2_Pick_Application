package org.example.db2_pick_app.service;

import org.example.db2_pick_app.model.MessageInfo;
import org.example.db2_pick_app.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service

public class CPaaSIntegrationService {
    private static final Logger logger = LoggerFactory.getLogger(CPaaSIntegrationService.class);

    private final MessageRepository messageRepository;
    private final ThreadPoolTaskExecutor messageProcessorExecutor;

    @Autowired
    public CPaaSIntegrationService(
            MessageRepository messageRepository,
            @Qualifier("messageProcessorExecutor") ThreadPoolTaskExecutor messageProcessorExecutor) {
        this.messageRepository = messageRepository;
        this.messageProcessorExecutor = messageProcessorExecutor;
    }

    public String sendMessagesInBatch(List<MessageInfo> messages) {
        try {
            String messageIdString = messages.stream()
                    .map(MessageInfo::getUniqueId)
                    .collect(Collectors.joining(","));

            logger.info("Processing batch of {} messages with ids: {}", messages.size(),messageIdString);
            messageRepository.updateMessageStatusBatch(messageIdString, 1);

            return messageIdString;
        } catch (Exception e) {
            logger.error("Failed to process message batch: {}", e.getMessage());
            throw new RuntimeException("Message batch processing failed", e);
        }
    }
}