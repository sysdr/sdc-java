package com.example.producer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LogGeneratorService {
    private static final Logger log = LoggerFactory.getLogger(LogGeneratorService.class);

    private final WebClient webClient;
    private final Random random = new Random();
    private final AtomicLong logCounter = new AtomicLong(0);
    private final Counter generatedLogsCounter;
    private final Counter sentLogsCounter;

    private final String routerUrl;
    private final int batchSize;

    private static final String[] SERVICES = {"user-service", "order-service", "payment-service", "inventory-service"};
    private static final String[] LEVELS = {"INFO", "WARN", "ERROR", "DEBUG"};
    private static final String[] MESSAGES = {
            "Request processed successfully",
            "Database connection timeout",
            "Cache miss for key",
            "Payment transaction completed",
            "User authentication failed",
            "Order placed successfully",
            "Inventory updated",
            "API rate limit exceeded"
    };

    public LogGeneratorService(
            WebClient.Builder webClientBuilder,
            @Value("${router.url}") String routerUrl,
            @Value("${log.generation.batch.size:100}") int batchSize,
            MeterRegistry meterRegistry) {
        
        this.webClient = webClientBuilder.build();
        this.routerUrl = routerUrl;
        this.batchSize = batchSize;

        this.generatedLogsCounter = Counter.builder("producer.logs.generated")
                .description("Total logs generated")
                .register(meterRegistry);
        this.sentLogsCounter = Counter.builder("producer.logs.sent")
                .description("Total logs sent to router")
                .register(meterRegistry);
    }

    @Scheduled(fixedRate = 1000)
    public void generateAndSendLogs() {
        Flux.range(0, batchSize)
                .map(i -> generateLog())
                .flatMap(this::sendLog, 10) // Concurrency of 10
                .subscribe(
                        response -> {
                            sentLogsCounter.increment();
                            log.debug("Log sent: {}", response);
                        },
                        error -> log.error("Failed to send log: {}", error.getMessage())
                );
    }

    private LogEntry generateLog() {
        generatedLogsCounter.increment();
        long id = logCounter.incrementAndGet();

        return new LogEntry(
                "log_" + id,
                "tenant_" + (random.nextInt(10) + 1),
                System.currentTimeMillis(),
                LEVELS[random.nextInt(LEVELS.length)],
                MESSAGES[random.nextInt(MESSAGES.length)] + " [" + id + "]",
                SERVICES[random.nextInt(SERVICES.length)]
        );
    }

    private Mono<String> sendLog(LogEntry logEntry) {
        return webClient.post()
                .uri(routerUrl + "/api/route")
                .bodyValue(logEntry)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(e -> {
                    log.error("Failed to send log {}: {}", logEntry.getLogId(), e.getMessage());
                    return Mono.empty();
                });
    }
}
