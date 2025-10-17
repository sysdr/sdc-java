package com.example.logprocessor.gateway;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class GatewayController {
    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);
    
    private final RestTemplate restTemplate;
    private final String producerUrl;
    private final Counter requestCounter;
    private final Timer requestTimer;

    public GatewayController(
            RestTemplate restTemplate,
            @Value("${services.producer.url}") String producerUrl,
            MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.producerUrl = producerUrl;
        this.requestCounter = Counter.builder("gateway.requests")
            .description("Total gateway requests")
            .register(meterRegistry);
        this.requestTimer = Timer.builder("gateway.request.duration")
            .description("Gateway request duration")
            .register(meterRegistry);
    }

    @PostMapping("/logs")
    @CircuitBreaker(name = "producer-service", fallbackMethod = "fallbackIngestLog")
    public ResponseEntity<Map<String, Object>> ingestLog(@RequestBody Map<String, Object> logData) {
        return requestTimer.record(() -> {
            requestCounter.increment();
            
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(logData, headers);
                
                ResponseEntity<Map> response = restTemplate.postForEntity(
                    producerUrl + "/api/logs", 
                    request, 
                    Map.class
                );
                
                logger.info("Log ingested successfully via gateway");
                return ResponseEntity.status(response.getStatusCode())
                    .body(Map.of(
                        "status", "success",
                        "data", response.getBody()
                    ));
            } catch (Exception e) {
                logger.error("Error forwarding log to producer", e);
                throw new RuntimeException("Failed to ingest log", e);
            }
        });
    }

    private ResponseEntity<Map<String, Object>> fallbackIngestLog(
            Map<String, Object> logData, Exception ex) {
        logger.warn("Circuit breaker activated, using fallback");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "status", "error",
                "message", "Service temporarily unavailable",
                "fallback", true
            ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "api-gateway"));
    }
}
