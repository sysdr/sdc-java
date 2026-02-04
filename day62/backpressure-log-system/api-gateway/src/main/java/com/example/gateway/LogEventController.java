package com.example.gateway;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/logs")
public class LogEventController {

    private final KafkaProducerService producerService;
    private final RateLimiter rateLimiter;
    private final LagMonitoringService lagMonitoringService;

    public LogEventController(KafkaProducerService producerService,
                            RateLimiter rateLimiter,
                            LagMonitoringService lagMonitoringService) {
        this.producerService = producerService;
        this.rateLimiter = rateLimiter;
        this.lagMonitoringService = lagMonitoringService;
    }

    @PostMapping
    public Mono<ResponseEntity<LogResponse>> publishLog(@RequestBody LogEvent event) {
        return Mono.fromCallable(() -> event)
            .transformDeferred(RateLimiterOperator.of(rateLimiter))
            .flatMap(producerService::sendLog)
            .map(result -> ResponseEntity.ok(new LogResponse("Log accepted", result)))
            .onErrorResume(RequestNotPermitted.class, e -> {
                log.warn("Rate limit exceeded. Current lag: {}", lagMonitoringService.getCurrentLag());
                return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new LogResponse("Rate limit exceeded - system under load", null)));
            })
            .onErrorResume(e -> {
                log.error("Error publishing log", e);
                return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new LogResponse("Service temporarily unavailable", null)));
            });
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<HealthResponse>> health() {
        long lag = lagMonitoringService.getCurrentLag();
        String status = lag < 10000 ? "healthy" : lag < 50000 ? "degraded" : "critical";
        
        return Mono.just(ResponseEntity.ok(
            new HealthResponse(status, lag, rateLimiter.getMetrics().getAvailablePermissions())
        ));
    }
}
