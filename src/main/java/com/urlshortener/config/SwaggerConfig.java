package com.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener Service API")
                        .version("1.0.0")
                        .description("A high-performance, production-quality URL shortener RESTful service built with Spring Boot 3.x, H2 in-memory database, and Base62 encoding.")
                        .contact(new Contact()
                                .name("Technical Interview Portfolio")
                                .email("candidate@example.com")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("URL Shortener API")
                .pathsToMatch("/api/**", "/{shortCode}")
                .build();
    }
}
