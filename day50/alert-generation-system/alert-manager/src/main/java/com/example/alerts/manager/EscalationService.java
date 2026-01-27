package com.example.alerts.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EscalationService {

    private final AlertHistoryRepository alertHistoryRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 60000) // Check every minute
    public void checkForEscalation() {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        
        // Find unacknowledged CRITICAL alerts older than 5 minutes
        List<AlertHistory> unacknowledgedAlerts = alertHistoryRepository
            .findByStateAndTriggeredAtBefore(
                AlertHistory.AlertState.NOTIFIED,
                fiveMinutesAgo
            );

        for (AlertHistory alert : unacknowledgedAlerts) {
            if ("CRITICAL".equals(alert.getSeverity())) {
                log.warn("Escalating unacknowledged alert: {}", alert.getAlertId());
                
                // Send escalation notification
                String escalationMessage = String.format(
                    "ESCALATION: Alert %s has been unacknowledged for 5+ minutes. " +
                    "Service: %s, Message: %s",
                    alert.getAlertId(),
                    alert.getService(),
                    alert.getMessage()
                );
                
                kafkaTemplate.send("notifications", "ESCALATION", escalationMessage);
            }
        }
    }
}
