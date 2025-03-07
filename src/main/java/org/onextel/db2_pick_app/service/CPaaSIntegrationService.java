package org.onextel.db2_pick_app.service;

import org.onextel.db2_pick_app.dto.SmsRequest;
import org.onextel.db2_pick_app.model.MessageInfo;
import org.onextel.db2_pick_app.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.stream.Collectors;


@Service

public class CPaaSIntegrationService {
    private static final Logger logger = LoggerFactory.getLogger(CPaaSIntegrationService.class);

    private final MessageRepository messageRepository;
    private final ThreadPoolTaskExecutor messageProcessorExecutor;
    private final WebClient webClient;

    @Autowired
    public CPaaSIntegrationService(
            MessageRepository messageRepository,
            @Qualifier("messageProcessorExecutor") ThreadPoolTaskExecutor messageProcessorExecutor,
            WebClient.Builder webClientBuilder) {
        this.messageRepository = messageRepository;
        this.messageProcessorExecutor = messageProcessorExecutor;
        this.webClient = webClientBuilder.baseUrl("https://api.smsc.ai").build();
    }

    public String sendMessagesInBatch(List<MessageInfo> messages) {
        try {
            String messageIdString = messages.stream()
                    .map(MessageInfo::getUniqueId)
                    .collect(Collectors.joining(","));

            logger.info("Processing batch of {} messages with ids: {}", messages.size(),messageIdString);
            messageRepository.updateMessageStatusBatch(messageIdString, 1);

            List<SmsRequest.SmsDetail> smsRequests = messages.stream()
                    .map(message -> new SmsRequest.SmsDetail(
                            "ONEXTEL",
                            message.getRecipientMobile(),
                            message.getMessageContent(),
                            message.getUniqueId()
                    ))
                    .toList();

            //Create the request body
            SmsRequest requestBody = new SmsRequest("sCYmf0y9", smsRequests);

            logger.info(requestBody.toString());

            String response = webClient.post()
                    .uri("api/jsmslist")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(res -> logger.info("Response: {}", res))
                    .doOnError(WebClientResponseException.class, ex -> logger.error("Error response: {}", ex.getResponseBodyAsString()))
                    .block();

            logger.info("Response body: {}", response);

            return messageIdString;
        } catch (Exception e) {
            throw new RuntimeException("Message batch processing failed", e);
        }
    }
}