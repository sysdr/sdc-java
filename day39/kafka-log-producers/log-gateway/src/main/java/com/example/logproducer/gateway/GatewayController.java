package com.example.logproducer.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GatewayController {
    
    private final WebClient.Builder webClientBuilder;
    
    @PostMapping("/logs")
    public Mono<Map<String, Object>> forwardToLogShipper(@RequestBody Map<String, Object> logData) {
        return webClientBuilder.build()
            .post()
            .uri("http://application-log-shipper:8081/api/v1/logs/ingest")
            .bodyValue(logData)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
    
    @PostMapping("/transactions")
    public Mono<Map<String, Object>> forwardToTransactionShipper(@RequestBody Map<String, Object> txData) {
        return webClientBuilder.build()
            .post()
            .uri("http://transaction-log-shipper:8083/api/v1/transactions")
            .bodyValue(txData)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
    
    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("status", "UP"));
    }
}
