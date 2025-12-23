package com.example.loadgen.service;

import com.example.loadgen.model.LoadTestConfig;
import com.example.loadgen.model.LoadTestResult;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Orchestrates load testing scenarios
 * Implements token bucket rate limiting to prevent drift
 */
@Slf4j
@Service
public class LoadTestOrchestrator {
    
    private final WebClient webClient;
    private final ExecutorService executor;
    
    // Metrics tracking
    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong successfulRequests = new AtomicLong();
    private final AtomicLong failedRequests = new AtomicLong();
    private final List<Long> latencies = new CopyOnWriteArrayList<>();
    private final Map<Integer, AtomicLong> statusCodes = new ConcurrentHashMap<>();
    
    public LoadTestOrchestrator() {
        this.webClient = WebClient.builder().build();
        this.executor = Executors.newFixedThreadPool(100);
    }
    
    /**
     * Execute burst load test scenario
     */
    public LoadTestResult executeBurstTest(LoadTestConfig config) {
        log.info("Starting burst load test: {}", config.getTestName());
        
        resetMetrics();
        Instant testStart = Instant.now();
        
        try {
            // Phase 1: Baseline load
            log.info("Phase 1: Baseline load {} req/sec for {}", 
                config.getBaselineRatePerSecond(), config.getBaselineDuration());
            generateSustainedLoad(config.getTargetUrl(), 
                config.getBaselineRatePerSecond(), 
                config.getBaselineDuration());
            
            // Phase 2: Burst
            log.info("Phase 2: Burst load {} req/sec for {}", 
                config.getBurstRatePerSecond(), config.getBurstDuration());
            Instant burstStart = Instant.now();
            generateSustainedLoad(config.getTargetUrl(), 
                config.getBurstRatePerSecond(), 
                config.getBurstDuration());
            
            // Phase 3: Recovery observation
            log.info("Phase 3: Recovery observation {} req/sec for {}", 
                config.getBaselineRatePerSecond(), config.getBaselineDuration());
            generateSustainedLoad(config.getTargetUrl(), 
                config.getBaselineRatePerSecond(), 
                config.getBaselineDuration());
            
            Instant testEnd = Instant.now();
            
            return buildTestResult(config.getTestName(), testStart, testEnd);
            
        } catch (Exception e) {
            log.error("Load test failed", e);
            return LoadTestResult.builder()
                .testName(config.getTestName())
                .testPassed(false)
                .failureReason(e.getMessage())
                .build();
        }
    }
    
    /**
     * Execute gradual ramp test scenario
     */
    public LoadTestResult executeRampTest(LoadTestConfig config) {
        log.info("Starting ramp load test: {}", config.getTestName());
        
        resetMetrics();
        Instant testStart = Instant.now();
        
        try {
            int startRate = config.getBaselineRatePerSecond();
            int endRate = config.getBurstRatePerSecond();
            Duration rampDuration = config.getRampDuration();
            
            // Calculate step size and interval
            int steps = (int) (rampDuration.getSeconds() / 10); // 10-second intervals
            int rateIncrement = (endRate - startRate) / steps;
            
            log.info("Ramping from {} to {} req/sec over {} seconds", 
                startRate, endRate, rampDuration.getSeconds());
            
            int currentRate = startRate;
            for (int i = 0; i < steps; i++) {
                generateSustainedLoad(config.getTargetUrl(), 
                    currentRate, 
                    Duration.ofSeconds(10));
                currentRate += rateIncrement;
                log.info("Ramp step {}/{}: {} req/sec", i + 1, steps, currentRate);
            }
            
            Instant testEnd = Instant.now();
            
            return buildTestResult(config.getTestName(), testStart, testEnd);
            
        } catch (Exception e) {
            log.error("Ramp test failed", e);
            return LoadTestResult.builder()
                .testName(config.getTestName())
                .testPassed(false)
                .failureReason(e.getMessage())
                .build();
        }
    }
    
    /**
     * Generate sustained load at specified rate
     * Uses Guava RateLimiter to prevent timer drift
     */
    private void generateSustainedLoad(String url, int ratePerSecond, Duration duration) {
        RateLimiter rateLimiter = RateLimiter.create(ratePerSecond);
        Instant start = Instant.now();
        Instant end = start.plus(duration);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        while (Instant.now().isBefore(end)) {
            rateLimiter.acquire();
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                sendRequest(url);
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        log.info("Completed sustained load: {} requests", futures.size());
    }
    
    /**
     * Send single HTTP request and track metrics
     */
    private void sendRequest(String url) {
        totalRequests.incrementAndGet();
        long startNs = System.nanoTime();
        
        try {
            // Generate random log event
            String logEvent = generateLogEvent();
            
            webClient.post()
                .uri(url)
                .bodyValue(logEvent)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(response -> {
                    long latencyMs = (System.nanoTime() - startNs) / 1_000_000;
                    latencies.add(latencyMs);
                    successfulRequests.incrementAndGet();
                    
                    int statusCode = response.getStatusCode().value();
                    statusCodes.computeIfAbsent(statusCode, k -> new AtomicLong())
                        .incrementAndGet();
                })
                .doOnError(error -> {
                    failedRequests.incrementAndGet();
                    log.warn("Request failed: {}", error.getMessage());
                })
                .block();
                
        } catch (Exception e) {
            failedRequests.incrementAndGet();
        }
    }
    
    /**
     * Generate random log event for testing
     */
    private String generateLogEvent() {
        String[] levels = {"INFO", "WARN", "ERROR", "DEBUG"};
        String[] services = {"api-gateway", "user-service", "order-service", "payment-service"};
        
        return String.format(
            "{\"timestamp\":\"%s\",\"level\":\"%s\",\"service\":\"%s\",\"message\":\"Load test event %d\"}",
            Instant.now(),
            levels[ThreadLocalRandom.current().nextInt(levels.length)],
            services[ThreadLocalRandom.current().nextInt(services.length)],
            ThreadLocalRandom.current().nextLong(1000000)
        );
    }
    
    /**
     * Build test result with calculated metrics
     */
    private LoadTestResult buildTestResult(String testName, Instant start, Instant end) {
        long duration = Duration.between(start, end).toSeconds();
        double avgThroughput = totalRequests.get() / (double) duration;
        
        // Calculate latency percentiles
        Collections.sort(latencies);
        double avgLatency = latencies.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        Map<Integer, Long> statusDistribution = new HashMap<>();
        statusCodes.forEach((code, count) -> statusDistribution.put(code, count.get()));
        
        boolean passed = failedRequests.get() < (totalRequests.get() * 0.01); // 1% error threshold
        
        return LoadTestResult.builder()
            .testName(testName)
            .startTime(start)
            .endTime(end)
            .totalRequests(totalRequests.get())
            .successfulRequests(successfulRequests.get())
            .failedRequests(failedRequests.get())
            .avgThroughput(avgThroughput)
            .peakThroughput(avgThroughput * 1.2) // Approximate
            .avgLatencyMs(avgLatency)
            .p50LatencyMs(calculatePercentile(latencies, 0.50))
            .p95LatencyMs(calculatePercentile(latencies, 0.95))
            .p99LatencyMs(calculatePercentile(latencies, 0.99))
            .maxLatencyMs(latencies.isEmpty() ? 0 : Collections.max(latencies))
            .statusCodeDistribution(statusDistribution)
            .testPassed(passed)
            .failureReason(passed ? null : "Error rate exceeded 1% threshold")
            .build();
    }
    
    /**
     * Calculate percentile from sorted list
     */
    private double calculatePercentile(List<Long> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) return 0.0;
        
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        
        return sortedValues.get(index);
    }
    
    /**
     * Reset metrics for new test
     */
    private void resetMetrics() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        latencies.clear();
        statusCodes.clear();
    }
}
