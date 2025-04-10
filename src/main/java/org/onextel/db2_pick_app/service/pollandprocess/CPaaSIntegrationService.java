package org.onextel.db2_pick_app.service.pollandprocess;

import org.onextel.db2_pick_app.client.RestWebClient;
import org.onextel.db2_pick_app.dto.PendingSmsDto;
import org.onextel.db2_pick_app.dto.SmsRequest;
import lombok.extern.slf4j.Slf4j;
import org.onextel.db2_pick_app.dto.SmsResponse;
import org.onextel.db2_pick_app.transformer.PendingSmsConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;



@Service
@Slf4j
public class CPaaSIntegrationService {
    private final RestWebClient restWebClient;

    @Value("${api.key:sCYmf0y9}")
    private String apiAuthToken;


    @Value("${sms.sender.id:ONEXTEL}")
    private String smsSenderId;

    @Value("${sms.api.base.url:https://api.smsc.ai}")
    private String baseUrl;

    PendingSmsConverter pendingSmsConverter;


    public CPaaSIntegrationService(RestWebClient restWebClient, PendingSmsConverter pendingSmsConverter) {
        this.restWebClient = restWebClient;
        this.pendingSmsConverter = pendingSmsConverter;
    }

    public Boolean sendMessagesInBatch(List<PendingSmsDto> messages) {
//        String messageIdString = messages.stream()
//                .map(dto -> String.valueOf(dto.getSrNo()))
//                .collect(Collectors.joining(","));

//        log.info("Processing batch of {} messages with ids: {}", messages.size(), messageIdString);

        List<SmsRequest.SmsDetail> smsRequests = pendingSmsConverter.createSmsDetails(messages);

        SmsRequest requestBody = new SmsRequest(apiAuthToken, smsRequests);
        log.info("Sending request: {}", requestBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<SmsResponse> responseEntity = restWebClient.execute(
                    baseUrl,
                    "/api/jsmslist",
                    SmsResponse.class,
                    null, // No query parameters
                    headers,
                    HttpMethod.POST,
                    null, // No path variables
                    requestBody
            ).block();

            log.info("API response received successfully: {}", responseEntity.getBody());
            return true;
        } catch (Exception ex) {
            log.error("Error occurred while sending messages in batch: {}", ex.getMessage(), ex);
            return false;
        }
    }




}