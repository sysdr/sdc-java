package com.example.logprocessor.gateway.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
public class GatewayController {

    private final WebClient normalizerClient;
    private final WebClient producerClient;

    public GatewayController(
            @Value("${app.services.normalizer-url}") String normalizerUrl,
            @Value("${app.services.producer-url}") String producerUrl) {
        this.normalizerClient = WebClient.builder().baseUrl(normalizerUrl).build();
        this.producerClient = WebClient.builder().baseUrl(producerUrl).build();
    }

    @PostMapping("/normalize")
    @CircuitBreaker(name = "normalizer", fallbackMethod = "normalizeFallback")
    public Mono<ResponseEntity<String>> normalize(
            @RequestBody String body,
            @RequestParam String targetFormat) {

        return normalizerClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/normalize/json")
                        .queryParam("targetFormat", targetFormat)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/produce")
    @CircuitBreaker(name = "producer", fallbackMethod = "produceFallback")
    public Mono<ResponseEntity<String>> produce(
            @RequestParam(defaultValue = "100") int count,
            @RequestParam(defaultValue = "JSON") String format) {

        return producerClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/producer/send")
                        .queryParam("count", count)
                        .queryParam("format", format)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "api-gateway"
        ));
    }

    public Mono<ResponseEntity<String>> normalizeFallback(String body, String format, Throwable t) {
        log.warn("Normalizer circuit breaker triggered: {}", t.getMessage());
        return Mono.just(ResponseEntity.status(503)
                .body("{\"error\": \"Normalizer service unavailable\"}"));
    }

    public Mono<ResponseEntity<String>> produceFallback(int count, String format, Throwable t) {
        log.warn("Producer circuit breaker triggered: {}", t.getMessage());
        return Mono.just(ResponseEntity.status(503)
                .body("{\"error\": \"Producer service unavailable\"}"));
    }
}
