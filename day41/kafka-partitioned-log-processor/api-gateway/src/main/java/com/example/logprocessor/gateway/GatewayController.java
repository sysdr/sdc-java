package com.example.logprocessor.gateway;

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

    @Value("${services.producer.url}")
    private String producerUrl;

    @Value("${services.consumer.url}")
    private String consumerUrl;

    @PostMapping("/logs")
    public Mono<ResponseEntity<Map>> createLog(@RequestBody Map<String, Object> request) {
        return webClientBuilder.build()
                .post()
                .uri(producerUrl + "/api/logs")
                .bodyValue(request)
                .retrieve()
                .toEntity(Map.class)
                .doOnSuccess(response -> 
                    log.info("Log created via gateway: {}", response.getBody()));
    }

    @GetMapping("/partition-mapping")
    public Mono<ResponseEntity<Map>> getPartitionMapping() {
        return webClientBuilder.build()
                .get()
                .uri(producerUrl + "/api/logs/partition-mapping")
                .retrieve()
                .toEntity(Map.class);
    }

    @GetMapping("/consumer/health")
    public Mono<ResponseEntity<Map>> getConsumerHealth() {
        return webClientBuilder.build()
                .get()
                .uri(consumerUrl + "/api/consumer/health")
                .retrieve()
                .toEntity(Map.class);
    }

    @GetMapping("/consumer/lag")
    public Mono<ResponseEntity<Map>> getConsumerLag() {
        return webClientBuilder.build()
                .get()
                .uri(consumerUrl + "/api/consumer/lag")
                .retrieve()
                .toEntity(Map.class);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Gateway healthy");
    }
}
