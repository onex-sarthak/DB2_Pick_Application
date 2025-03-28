package org.onextel.db2_pick_app.service;

import org.onextel.db2_pick_app.dto.SmsRequest;
import org.onextel.db2_pick_app.model.MessageInfo;
import org.onextel.db2_pick_app.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class CPaaSIntegrationService {
    private final WebClient webClient;

    @Value("${api.auth.token:sCYmf0y9}")
    private String apiAuthToken;


    @Value("${sms.sender.id:ONEXTEL}")
    private String smsSenderId;


    @Autowired
    public CPaaSIntegrationService(
            WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.smsc.ai").build();
    }

    public boolean sendMessagesInBatch(List<MessageInfo> messages) throws Throwable {
        try {
            String messageIdString = messages.stream()
                    .map(MessageInfo::getUniqueId)
                    .collect(Collectors.joining(","));

            log.info("Processing batch of {} messages with ids: {}", messages.size(),messageIdString);
//            messageRepository.updateMessageStatusBatch(messageIdString, 1);

            List<SmsRequest.SmsDetail> smsRequests = messages.stream()
                    .map(this::createSmsDetail)
                    .toList();

            //Create the request body
            SmsRequest requestBody = new SmsRequest(apiAuthToken, smsRequests);

            log.info("Sending request : {}",requestBody.toString());


            // This needs to be verified for retry...
            String response = webClient.post()
                    .uri("api/jsmslist")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(
                            Retry.fixedDelay(3, Duration.ofSeconds(5)) // Retry up to 3 times with a 5-second delay
                                    .filter(throwable -> throwable instanceof WebClientResponseException)
                    )
                    .doOnSuccess(res -> log.info("API response received successfully: {}", res))
                    .doOnError(WebClientResponseException.class, ex -> log.error("Error response: {}", ex.getResponseBodyAsString()))
                    .toString();

            return true;
        } catch (WebClientResponseException e) {
            log.error("Error response: {}", e.getResponseBodyAsString());
            return false;
        }
        catch (Exception e) {
            throw new RuntimeException("Message batch processing failed", e);
        }
    }


    private SmsRequest.SmsDetail createSmsDetail(MessageInfo message) {
        return new SmsRequest.SmsDetail(
                smsSenderId,
                message.getRecipientMobileNumber(),
                message.getMessageContent(),
                message.getUniqueId()
        );
    }
}