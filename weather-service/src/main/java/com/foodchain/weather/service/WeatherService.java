package com.foodchain.weather.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class WeatherService {

    private final WebClient weatherWebClient;
    private final String apiKey;

    public WeatherService(WebClient weatherWebClient,
                          @Value("${app.weather.api-key}") String apiKey) {
        this.weatherWebClient = weatherWebClient;
        this.apiKey = apiKey;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getWeather(double lat, double lon, String units) {
        return weatherWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/data/2.5/weather")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .queryParam("appid", apiKey)
                        .queryParam("units", units)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}
