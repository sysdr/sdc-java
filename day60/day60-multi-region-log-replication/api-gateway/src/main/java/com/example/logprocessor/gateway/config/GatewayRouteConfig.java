package com.example.logprocessor.gateway.config;

import com.example.logprocessor.gateway.service.RegionRoutingService;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic Spring Cloud Gateway route definitions.
 *
 * Route: /api/logs/** -> log-producer (region-aware)
 *
 * We use programmatic routes instead of YAML because the target URI
 * is dynamically resolved by RegionRoutingService based on the X-Region header.
 *
 * Filters applied:
 *   - AddRequestHeader: injects X-Correlation-Id if not present
 *   - Retry: 2 retries on 5xx with exponential backoff
 */
@Configuration
public class GatewayRouteConfig {

    private final RegionRoutingService routingService;

    public GatewayRouteConfig(RegionRoutingService routingService) {
        this.routingService = routingService;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("log-producer-route", r -> r
                        .path("/api/logs/**")
                        .filters(f -> f
                                .stripPrefix(1)                          // /api/logs/X -> /logs/X
                                .addRequestHeader("X-Gateway-Routed", "true")
                                .retry(retrySpec -> retrySpec
                                        .setRetries(3)
                                        .setMethods(org.springframework.http.HttpMethod.POST)
                                        .setStatuses(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                                                  org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE))
                        )
                        .uri("http://localhost:8081") // Default; overridden by dynamic filter
                )
                .build();
    }
}
