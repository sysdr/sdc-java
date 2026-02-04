package com.example.consumer;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@Slf4j
@Service
public class LogProcessingService {
    private static final Logger log = LoggerFactory.getLogger(LogProcessingService.class);

    private final LogRepository logRepository;
    private final CircuitBreaker databaseCircuitBreaker;

    public LogProcessingService(LogRepository logRepository,
                               CircuitBreaker databaseCircuitBreaker) {
        this.logRepository = logRepository;
        this.databaseCircuitBreaker = databaseCircuitBreaker;
    }

    public Mono<String> processLog(LogEvent event) {
        return Mono.fromCallable(() -> {
                // Simulate processing time based on severity
                if ("ERROR".equals(event.getSeverity())) {
                    Thread.sleep(50);  // Critical logs processed slower
                } else if ("DEBUG".equals(event.getSeverity())) {
                    Thread.sleep(10);  // Debug logs processed faster
                }
                
                LogEntity entity = new LogEntity();
                entity.setCorrelationId(event.getCorrelationId());
                entity.setSeverity(event.getSeverity());
                entity.setMessage(event.getMessage());
                entity.setSource(event.getSource());
                entity.setTimestamp(event.getTimestamp());
                entity.setProcessedAt(System.currentTimeMillis());
                
                return logRepository.save(entity);
            })
            .transformDeferred(CircuitBreakerOperator.of(databaseCircuitBreaker))
            .map(entity -> "PROCESSED:" + entity.getId())
            .timeout(Duration.ofSeconds(5))
            .doOnError(e -> log.error("Processing error for {}", event.getCorrelationId(), e));
    }
}
