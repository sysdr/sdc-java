package com.example.apigateway;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnomalyStats {
    private long totalAnomalies;
    private long last24Hours;
    private long highConfidence;
}
