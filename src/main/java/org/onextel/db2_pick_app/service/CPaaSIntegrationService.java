package org.onextel.db2_pick_app.service;

import org.onextel.db2_pick_app.dto.PendingSmsDto;
import org.onextel.db2_pick_app.dto.SmsRequest;
import org.onextel.db2_pick_app.model.MessageInfo;
//import org.onextel.db2_pick_app.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
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

    public Mono<Boolean> sendMessagesInBatch(List<PendingSmsDto> messages) {
        String messageIdString = messages.stream()
                .map(dto -> String.valueOf(dto.getSrNo()))
                .collect(Collectors.joining(","));

        log.info("Processing batch of {} messages with ids: {}", messages.size(), messageIdString);

        List<SmsRequest.SmsDetail> smsRequests = messages.stream()
                .map(this::createSmsDetail)
                .toList();

        SmsRequest requestBody = new SmsRequest(apiAuthToken, smsRequests);
        log.info("Sending request: {}", requestBody);

        return webClient.post()
                .uri("/api/jsmslist")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.error("Client error (4xx) occurred: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                            .doOnNext(body -> log.error("Response body: {}", body))
                            .then(Mono.error(new RuntimeException("4xx error: " + response.statusCode())));
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    log.error("Server error (5xx) occurred: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                            .doOnNext(body -> log.error("Response body: {}", body))
                            .then(Mono.error(new RuntimeException("5xx error: " + response.statusCode())));
                })
                .bodyToMono(String.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(5))
                        .filter(throwable -> throwable instanceof WebClientResponseException))
                .doOnSuccess(response -> log.info("API response received successfully: {}", response))
                .doOnError(WebClientResponseException.class, ex -> log.error("Error response: {}", ex.getResponseBodyAsString()))
                .map(response -> true) // Convert response to boolean
                .onErrorReturn(false); // Return false if an error occurs
    }



    private SmsRequest.SmsDetail createSmsDetail(PendingSmsDto message) {
        return new SmsRequest.SmsDetail(
                smsSenderId,
                message.getDestination(),
                message.getMessage(),
                String.valueOf(message.getSrNo())
        );
    }
}