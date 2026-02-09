package com.foodchain.weather.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${app.weather.base-url}")
    private String weatherBaseUrl;

    @Bean
    public WebClient weatherWebClient() {
        return WebClient.builder()
                .baseUrl(weatherBaseUrl)
                .build();
    }
}
