package com.event.tracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean(name = "ticketmasterWebClient")
    public WebClient ticketmasterWebClient() {
        return WebClient.builder()
                .baseUrl("https://app.ticketmaster.com/discovery/v2")
                .build();
    }

    @Bean(name = "holidayWebClient")
    public WebClient holidayWebClient() {
        return WebClient.builder()
                .baseUrl("https://date.nager.at/api/v3")
                .build();
    }
}
