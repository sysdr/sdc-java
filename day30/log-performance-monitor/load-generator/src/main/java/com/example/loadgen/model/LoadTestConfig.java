package com.example.loadgen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

/**
 * Configuration for load test scenarios
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadTestConfig {
    private String testName;
    private int baselineRatePerSecond;
    private int burstRatePerSecond;
    private Duration baselineDuration;
    private Duration burstDuration;
    private Duration rampDuration;
    private String targetUrl;
    private int concurrentThreads;
}
