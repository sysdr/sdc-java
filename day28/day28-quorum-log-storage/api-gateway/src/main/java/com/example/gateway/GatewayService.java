package com.example.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gateway service coordinating with quorum coordinator
 */
@Service
@Slf4j
public class GatewayService {
    
    private final WebClient webClient;
    
    public GatewayService(@Value("${coordinator.url}") String coordinatorUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(coordinatorUrl)
            .build();
    }

    public Mono<Map<String, Object>> writeLog(String key, String value, String consistency) {
        return webClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/quorum/write")
                .queryParam("key", key)
                .queryParam("value", value)
                .queryParam("consistency", consistency)
                .build())
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> {
                Map<String, Object> result = new HashMap<>();
                result.put("success", response.get("success"));
                result.put("acknowledgedReplicas", response.get("acknowledgedReplicas"));
                result.put("requiredReplicas", response.get("requiredReplicas"));
                return result;
            })
            .onErrorResume(error -> {
                log.error("Write failed: {}", error.getMessage());
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("acknowledgedReplicas", 0);
                errorResult.put("requiredReplicas", 0);
                return Mono.just(errorResult);
            });
    }

    public Mono<Map<String, Object>> readLog(String key, String consistency) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/quorum/read")
                .queryParam("key", key)
                .queryParam("consistency", consistency)
                .build())
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> {
                Map<String, Object> result = new HashMap<>();
                
                if (response.get("success").equals(false)) {
                    result.put("success", false);
                    return result;
                }
                
                Map<String, Object> valueObj = (Map<String, Object>) response.get("value");
                List<Object> conflicts = (List<Object>) response.get("conflicts");
                
                result.put("success", true);
                result.put("value", valueObj != null ? valueObj.get("value") : null);
                result.put("conflicts", conflicts != null ? conflicts.size() : 0);
                
                return result;
            })
            .doOnError(error -> log.error("Read failed: {}", error.getMessage()));
    }
}
