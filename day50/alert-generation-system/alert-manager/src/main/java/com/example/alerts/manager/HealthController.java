package com.example.alerts.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final AlertHistoryRepository alertHistoryRepository;

    @GetMapping("/health")
    public Map<String, Object> health() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Long recentAlerts = alertHistoryRepository.countByStateAfter(
            AlertHistory.AlertState.TRIGGERED,
            oneHourAgo
        );

        return Map.of(
            "status", "UP",
            "service", "alert-manager",
            "version", "1.0.0",
            "alertsLastHour", recentAlerts
        );
    }
}
