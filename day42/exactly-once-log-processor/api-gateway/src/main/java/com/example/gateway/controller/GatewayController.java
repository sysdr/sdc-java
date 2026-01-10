package com.example.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class GatewayController {

    private final WebClient.Builder webClientBuilder;

    @Value("${producer.url}")
    private String producerUrl;

    @PostMapping("/logs")
    public Mono<ResponseEntity<Map>> forwardToProducer(@RequestBody Map<String, Object> logEvent) {
        log.info("Gateway forwarding log event to producer");
        
        return webClientBuilder.build()
                .post()
                .uri(producerUrl + "/api/logs")
                .bodyValue(logEvent)
                .retrieve()
                .toEntity(Map.class)
                .doOnSuccess(response -> log.info("Successfully forwarded to producer"))
                .doOnError(error -> log.error("Failed to forward to producer: {}", error.getMessage()));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "api-gateway"));
    }
}
