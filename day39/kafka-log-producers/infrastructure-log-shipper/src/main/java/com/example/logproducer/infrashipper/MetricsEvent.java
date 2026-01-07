package com.example.logproducer.infrashipper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsEvent {
    private String eventId;
    private String hostname;
    private String metricType;
    private Double value;
    private String unit;
    private Instant timestamp;
    private Map<String, String> tags;
}
