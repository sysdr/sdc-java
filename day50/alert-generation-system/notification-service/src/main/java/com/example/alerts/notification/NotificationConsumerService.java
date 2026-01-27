package com.example.alerts.notification;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumerService {

    private final FailedNotificationRepository failedNotificationRepository;
    private final MeterRegistry meterRegistry;

    @KafkaListener(topics = "notifications", groupId = "notification-service-group")
    public void consumeNotification(String alertJson) {
        log.info("Processing notification: {}", alertJson);
        
        try {
            // Route based on severity
            if (alertJson.contains("CRITICAL") || alertJson.contains("ESCALATION")) {
                sendToPagerDuty(alertJson);
            } else if (alertJson.contains("WARNING")) {
                sendToSlack(alertJson);
            } else {
                sendToEmail(alertJson);
            }
            
            incrementCounter("notifications.sent", "success");
            
        } catch (Exception e) {
            log.error("Failed to send notification", e);
            incrementCounter("notifications.sent", "failure");
        }
    }

    @CircuitBreaker(name = "pagerduty", fallbackMethod = "pagerDutyFallback")
    public void sendToPagerDuty(String alert) {
        log.info("📟 Sending to PagerDuty: {}", alert);
        
        // Simulate PagerDuty API call
        if (Math.random() < 0.95) { // 95% success rate
            log.info("✅ PagerDuty notification sent successfully");
        } else {
            throw new RuntimeException("PagerDuty API unavailable");
        }
    }

    public void pagerDutyFallback(String alert, Exception e) {
        log.warn("⚠️ PagerDuty circuit open, using fallback to Slack");
        sendToSlack(alert);
        saveFailedNotification(alert, "pagerduty", e.getMessage());
    }

    @CircuitBreaker(name = "slack", fallbackMethod = "slackFallback")
    public void sendToSlack(String alert) {
        log.info("💬 Sending to Slack: {}", alert);
        
        // Simulate Slack API call
        if (Math.random() < 0.98) { // 98% success rate
            log.info("✅ Slack notification sent successfully");
        } else {
            throw new RuntimeException("Slack API rate limited");
        }
    }

    public void slackFallback(String alert, Exception e) {
        log.warn("⚠️ Slack circuit open, using fallback to Email");
        sendToEmail(alert);
        saveFailedNotification(alert, "slack", e.getMessage());
    }

    @CircuitBreaker(name = "email", fallbackMethod = "emailFallback")
    public void sendToEmail(String alert) {
        log.info("📧 Sending email: {}", alert);
        
        // Simulate email sending
        log.info("✅ Email notification sent successfully");
    }

    public void emailFallback(String alert, Exception e) {
        log.error("❌ All notification channels failed for alert");
        saveFailedNotification(alert, "email", e.getMessage());
    }

    private void saveFailedNotification(String payload, String channel, String reason) {
        FailedNotification failed = FailedNotification.builder()
            .payload(payload)
            .channel(channel)
            .failureReason(reason)
            .retryCount(0)
            .failedAt(LocalDateTime.now())
            .nextRetryAt(LocalDateTime.now().plusMinutes(1))
            .status(FailedNotification.RetryStatus.PENDING)
            .build();
        
        failedNotificationRepository.save(failed);
        log.info("Saved failed notification for retry: {}", failed.getId());
    }

    private void incrementCounter(String name, String status) {
        Counter.builder(name)
            .tag("status", status)
            .register(meterRegistry)
            .increment();
    }
}
