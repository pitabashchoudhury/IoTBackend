package com.foodchain.iotbackend.controller;

import com.foodchain.iotbackend.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getWeather(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "metric") String units) {
        return ResponseEntity.ok(weatherService.getWeather(lat, lon, units));
    }
}
