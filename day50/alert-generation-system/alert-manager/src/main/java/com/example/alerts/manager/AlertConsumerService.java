package com.example.alerts.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertConsumerService {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AlertHistoryRepository alertHistoryRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @KafkaListener(topics = "alerts", groupId = "alert-manager-group")
    public void consumeAlert(String alertJson) {
        try {
            Alert alert = objectMapper.readValue(alertJson, Alert.class);
            log.info("Processing alert: {}", alert.getAlertId());

            // Check for duplicate alert using Redis
            String dedupeKey = "alert:active:" + alert.getFingerprint();
            Boolean isDuplicate = redisTemplate.hasKey(dedupeKey);

            if (Boolean.TRUE.equals(isDuplicate)) {
                log.info("Duplicate alert detected and suppressed: {}", alert.getFingerprint());
                incrementCounter("alerts.deduplicated", alert.getSeverity());
                return;
            }

            // Store alert in Redis with 5-minute TTL
            redisTemplate.opsForValue().set(
                dedupeKey, 
                alert.getAlertId(), 
                5, 
                TimeUnit.MINUTES
            );

            // Enrich alert with correlation data
            Alert enrichedAlert = enrichWithCorrelation(alert);

            // Save to database
            saveAlertHistory(enrichedAlert);

            // Send to notification service
            kafkaTemplate.send("notifications", enrichedAlert.getSeverity(), alertJson);
            
            incrementCounter("alerts.processed", alert.getSeverity());
            log.info("Alert sent to notification service: {}", alert.getAlertId());

        } catch (Exception e) {
            log.error("Error processing alert", e);
            incrementCounter("alerts.errors", "unknown");
        }
    }

    private Alert enrichWithCorrelation(Alert alert) {
        // Check for related alerts in the last 2 minutes
        String correlationKey = "alert:correlation:" + alert.getService();
        Long relatedCount = redisTemplate.opsForList().size(correlationKey);

        if (relatedCount != null && relatedCount > 0) {
            String originalMessage = alert.getMessage();
            alert.setMessage(String.format("%s (Related alerts: %d)", originalMessage, relatedCount));
        }

        // Track this alert for correlation
        redisTemplate.opsForList().rightPush(correlationKey, alert.getAlertId());
        redisTemplate.expire(correlationKey, 2, TimeUnit.MINUTES);

        return alert;
    }

    private void saveAlertHistory(Alert alert) {
        AlertHistory history = AlertHistory.builder()
            .alertId(alert.getAlertId())
            .ruleId(alert.getRuleId())
            .ruleName(alert.getRuleName())
            .service(alert.getService())
            .severity(alert.getSeverity())
            .message(alert.getMessage())
            .count(alert.getCount())
            .fingerprint(alert.getFingerprint())
            .state(AlertHistory.AlertState.TRIGGERED)
            .triggeredAt(LocalDateTime.now())
            .build();

        alertHistoryRepository.save(history);
    }

    private void incrementCounter(String name, String severity) {
        Counter.builder(name)
            .tag("severity", severity)
            .register(meterRegistry)
            .increment();
    }
}
