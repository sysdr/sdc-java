package com.example.logprocessor.gateway;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {
    
    /**
     * Configure routes to log-producer service
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Route for log ingestion
            .route("log_ingest_route", r -> r
                .path("/api/logs/ingest")
                .filters(f -> f
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(ipKeyResolver()))
                    .retry(config -> config.setRetries(3)))
                .uri("http://log-producer:8081"))
            
            // Route for batch ingestion
            .route("log_batch_route", r -> r
                .path("/api/logs/ingest/batch")
                .filters(f -> f
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(ipKeyResolver())))
                .uri("http://log-producer:8081"))
            
            // Health check route
            .route("health_route", r -> r
                .path("/api/logs/health")
                .uri("http://log-producer:8081"))
            
            .build();
    }
    
    /**
     * Configure rate limiting with Redis
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // Allow 1000 requests per second per IP
        return new RedisRateLimiter(1000, 2000, 1);
    }
    
    /**
     * Key resolver for rate limiting by IP address
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest()
                .getRemoteAddress()
                .getAddress()
                .getHostAddress()
        );
    }
    
}
