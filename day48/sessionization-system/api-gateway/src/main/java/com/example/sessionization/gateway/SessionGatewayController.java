package com.example.sessionization.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SessionGatewayController {
    private final RedisTemplate<String, String> redisTemplate;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public SessionGatewayController(
            RedisTemplate<String, String> redisTemplate,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.webClient = webClientBuilder.baseUrl("http://session-analytics:8083").build();
        this.objectMapper = objectMapper;
    }

    @GetMapping("/sessions/active/{userId}")
    public Mono<Map<String, Object>> getActiveSession(@PathVariable String userId) {
        return Mono.fromCallable(() -> {
            String key = "session:" + userId;
            String json = redisTemplate.opsForValue().get(key);
            
            Map<String, Object> response = new HashMap<>();
            if (json != null) {
                response.put("status", "active");
                response.put("session", objectMapper.readValue(json, Object.class));
            } else {
                response.put("status", "no_active_session");
            }
            return response;
        });
    }

    @GetMapping("/sessions/history/{userId}")
    public Mono<Object> getSessionHistory(@PathVariable String userId) {
        return webClient.get()
            .uri("/api/analytics/sessions/user/" + userId)
            .retrieve()
            .bodyToMono(Object.class);
    }

    @GetMapping("/analytics/stats")
    public Mono<Object> getStats(@RequestParam(defaultValue = "24") int hours) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/analytics/stats")
                .queryParam("hours", hours)
                .build())
            .retrieve()
            .bodyToMono(Object.class);
    }

    @GetMapping("/sessions/converted")
    public Mono<Object> getConvertedSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/analytics/sessions/converted")
                .queryParam("page", page)
                .queryParam("size", size)
                .build())
            .retrieve()
            .bodyToMono(Object.class);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "api-gateway");
        return health;
    }
}
