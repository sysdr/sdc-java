package com.example.alerts.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    private String alertId;
    private String ruleId;
    private String ruleName;
    private String service;
    private String severity;
    private String message;
    private Long count;
    private String timestamp;
    private String fingerprint;
}
