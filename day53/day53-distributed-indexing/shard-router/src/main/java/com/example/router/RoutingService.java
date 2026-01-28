package com.example.router;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class RoutingService {
    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

    private final ConsistentHashRing hashRing;
    private final WebClient webClient;
    private final Counter routedRequestsCounter;
    private final Counter failedRoutesCounter;

    public RoutingService(
            ConsistentHashRing hashRing,
            WebClient.Builder webClientBuilder,
            MeterRegistry meterRegistry) {
        
        this.hashRing = hashRing;
        this.webClient = webClientBuilder.build();
        
        this.routedRequestsCounter = Counter.builder("router.requests.routed")
                .description("Total requests routed")
                .register(meterRegistry);
        this.failedRoutesCounter = Counter.builder("router.requests.failed")
                .description("Failed routing attempts")
                .register(meterRegistry);
    }

    public Mono<String> routeLog(LogEntry logEntry) {
        routedRequestsCounter.increment();

        // Use logId for consistent hashing
        String targetNode = hashRing.getNode(logEntry.getLogId());
        
        if (targetNode == null) {
            log.error("No available nodes in hash ring");
            failedRoutesCounter.increment();
            return Mono.error(new RuntimeException("No available nodes"));
        }

        log.info("Routing log {} to node {}", logEntry.getLogId(), targetNode);

        return webClient.post()
                .uri(targetNode + "/api/index")
                .bodyValue(logEntry)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .doOnError(e -> {
                    log.error("Failed to route log {} to {}: {}", 
                            logEntry.getLogId(), targetNode, e.getMessage());
                    failedRoutesCounter.increment();
                })
                .doOnSuccess(response -> log.debug("Successfully routed log {} to {}", 
                        logEntry.getLogId(), targetNode));
    }
}
