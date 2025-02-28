package org.example.db2_pick_app.service;

import com.ibm.db2.cmx.runtime.internal.resources.Messages;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.example.db2_pick_app.dto.MessageSubmissionRequest;
import org.example.db2_pick_app.dto.MessageSubmissionResponse;
import org.example.db2_pick_app.model.MessageQueue;
import org.example.db2_pick_app.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.UUID;
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

    public String sendMessagesInBatch(List<MessageQueue> messages) {
        try {
            String messageIdString = messages.stream()
                    .map(MessageQueue::getUniqueId)
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