package com.example.logprocessor.gateway;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {
    
    private final WebClient.Builder webClientBuilder;
    private final MeterRegistry meterRegistry;

    @GetMapping("/throughput")
    public Mono<Map<String, Object>> getThroughput() {
        WebClient producerClient = webClientBuilder.baseUrl("http://localhost:8081").build();
        
        return producerClient.get()
                .uri("/actuator/metrics/kafka.producer.send.success")
                .retrieve()
                .bodyToMono(Map.class)
                .map(metrics -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("producer", metrics);
                    result.put("timestamp", System.currentTimeMillis());
                    return result;
                })
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @GetMapping("/latency")
    public Mono<Map<String, Object>> getLatency() {
        WebClient consumerClient = webClientBuilder.baseUrl("http://localhost:8082").build();
        
        return consumerClient.get()
                .uri("/actuator/metrics/log.processing.time")
                .retrieve()
                .bodyToMono(Map.class)
                .map(metrics -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("consumer", metrics);
                    result.put("timestamp", System.currentTimeMillis());
                    return result;
                })
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
    }

    @GetMapping("/summary")
    public Mono<Map<String, Object>> getSummary() {
        return Mono.zip(getThroughput(), getLatency())
                .map(tuple -> {
                    Map<String, Object> summary = new HashMap<>();
                    summary.put("throughput", tuple.getT1());
                    summary.put("latency", tuple.getT2());
                    return summary;
                });
    }
}
