package com.foodchain.weather.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI weatherServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Weather Service API")
                        .description("Weather Data proxy endpoints")
                        .version("1.0"));
    }
}
