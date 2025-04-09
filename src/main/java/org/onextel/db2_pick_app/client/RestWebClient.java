package org.onextel.db2_pick_app.client;
import reactor.util.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.Duration;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class RestWebClient {

    private final WebClient webClient;

    public RestWebClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public <T, U> ResponseEntity<T> execute(String baseUrl,
                                            String resourceUrl,
                                            Class<T> responseClassType,
                                            Map<String, Object> queryParam,
                                            HttpHeaders headers,
                                            HttpMethod httpMethod,
                                            Map<String, Object> pathVariables,
                                            U body) {

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(baseUrl + resourceUrl);

        if (pathVariables != null && !pathVariables.isEmpty()) {
            uriComponentsBuilder.uriVariables(pathVariables);
        }
        if (queryParam != null && !queryParam.isEmpty()) {
            queryParam.forEach(uriComponentsBuilder::queryParam);
        }

        String url = uriComponentsBuilder.toUriString();

        WebClient.RequestBodySpec requestBodySpec = webClient
                .method(httpMethod)
                .uri(url)
                .headers(httpHeaders -> httpHeaders.addAll(headers));

        try {
            return requestBodySpec.bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        return response.bodyToMono(String.class)
                                .doOnNext(bodyContent -> log.error("4xx Error: " + bodyContent))
                                .then(Mono.error(new RuntimeException("Client error: " + response.statusCode())));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        return response.bodyToMono(String.class)
                                .doOnNext(bodyContent -> System.err.println("5xx Error: " + bodyContent))
                                .then(Mono.error(new RuntimeException("Server error: " + response.statusCode())));
                    })
                    .toEntity(responseClassType)
                    .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(5))
                            .filter(throwable -> throwable instanceof WebClientResponseException))
                    .doOnSuccess(response -> System.out.println("Success: " + response.getBody()))
                    .doOnError(WebClientResponseException.class, ex -> System.err.println("Error: " + ex.getResponseBodyAsString()))
                    .block();
        } catch (Exception ex) {
            System.err.println("Exception occurred: " + ex.getMessage());
            throw ex;
        }
    }
}