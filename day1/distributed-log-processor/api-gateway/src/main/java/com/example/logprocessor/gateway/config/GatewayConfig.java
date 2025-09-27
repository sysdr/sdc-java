package com.example.logprocessor.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("log-producer", r -> r.path("/api/logs/**")
                        .uri("http://localhost:8081"))
                .route("log-consumer-health", r -> r.path("/consumer/health")
                        .uri("http://localhost:8082"))
                .build();
    }
}
