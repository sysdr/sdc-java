package com.example.query.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient encryptionServiceClient() {
        return WebClient.builder()
            .baseUrl("http://encryption-service:8081")
            .build();
    }
}
