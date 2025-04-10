package org.onextel.db2_pick_app.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public ConnectionProvider connectionProvider() {
        return ConnectionProvider.builder("web-client-connection-provider")
                .maxConnections(100)
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(2))
                .pendingAcquireTimeout(Duration.ofSeconds(2))
                .evictInBackground(Duration.ofMinutes(1))
                .build();
    }

    @Bean("customHttpClient")
    public HttpClient httpClient(ConnectionProvider connectionProvider){
        return HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .responseTimeout(Duration.ofSeconds(3));
    }

    @Bean("customWebClient")
    public WebClient webClient(@Qualifier("customHttpClient") HttpClient httpClient) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }



}
