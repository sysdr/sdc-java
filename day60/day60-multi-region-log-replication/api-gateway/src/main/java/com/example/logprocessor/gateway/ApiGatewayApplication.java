package com.example.logprocessor.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway entry point.
 *
 * Responsibilities:
 *   - Route incoming /logs requests to the correct regional Log Producer
 *   - Health-check both regions and fall back to the healthy one
 *   - Apply rate limiting and circuit breakers at the edge
 *   - Add distributed tracing headers (X-Correlation-Id)
 */
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
