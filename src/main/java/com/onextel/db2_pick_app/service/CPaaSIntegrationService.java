package com.onextel.db2_pick_app.service;

import com.onextel.db2_pick_app.dto.SmsRequest;
import com.onextel.db2_pick_app.model.MessageInfo;
import com.onextel.db2_pick_app.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class CPaaSIntegrationService {
    private final MessageRepository messageRepository;
    private final WebClient webClient;

    @Value("${api.auth.token:sCYmf0y9}")
    private String apiAuthToken;


    @Value("${sms.sender.id:ONEXTEL}")
    private String smsSenderId;


    @Autowired
    public CPaaSIntegrationService(
            MessageRepository messageRepository,
            WebClient.Builder webClientBuilder) {
        this.messageRepository = messageRepository;
        this.webClient = webClientBuilder.baseUrl("https://api.smsc.ai").build();
    }

    public String sendMessagesInBatch(List<MessageInfo> messages) {
        try {
            String messageIdString = messages.stream()
                    .map(MessageInfo::getUniqueId)
                    .collect(Collectors.joining(","));

            log.info("Processing batch of {} messages with ids: {}", messages.size(),messageIdString);
            messageRepository.updateMessageStatusBatch(messageIdString, 1);

            List<SmsRequest.SmsDetail> smsRequests = messages.stream()
                    .map(this::createSmsDetail)
                    .toList();

            //Create the request body
            SmsRequest requestBody = new SmsRequest(apiAuthToken, smsRequests);

            log.info("Sending request : {}",requestBody.toString());

            String response = webClient.post()
                    .uri("api/jsmslist")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(res -> log.info("API response received successfully"))
                    .doOnError(WebClientResponseException.class, ex -> log.error("Error response: {}", ex.getResponseBodyAsString()))
                    .block();

            log.info("API response : {}", response);

            return messageIdString;
        } catch (Exception e) {
            throw new RuntimeException("Message batch processing failed", e);
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

    private SmsRequest.SmsDetail createSmsDetail(MessageInfo message) {
        return new SmsRequest.SmsDetail(
                smsSenderId,
                message.getRecipientMobile(),
                message.getMessageContent(),
                message.getUniqueId()
        );
    }
}