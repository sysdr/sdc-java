package com.example.logprocessor.gateway.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Region health tracker and routing decision engine.
 *
 * Design decisions:
 *   1. Polling-based health checks every 5 seconds (configurable).
 *      We could use server-push, but polling is simpler and doesn't require
 *      a reverse connection from producers to the gateway.
 *   2. Health state is cached in a ConcurrentHashMap — no external dependency
 *      for routing decisions. Stale data (max 5s) is acceptable for routing.
 *   3. Sticky routing: once a request specifies a region (via X-Region header),
 *      that region is preferred as long as it's healthy.
 *
 * Fallback logic:
 *   Requested region healthy? -> Route there.
 *   Requested region unhealthy? -> Route to the other region + emit metric.
 *   Both unhealthy? -> Reject with 503.
 */
@Service
public class RegionRoutingService {

    private static final Logger log = LoggerFactory.getLogger(RegionRoutingService.class);

    private final WebClient webClient;
    private final Map<String, String> regionEndpoints;
    private final ConcurrentHashMap<String, Boolean> healthCache = new ConcurrentHashMap<>();
    private final Counter fallbackCounter;

    public RegionRoutingService(
            @Value("${app.region-a.endpoint}") String regionAEndpoint,
            @Value("${app.region-b.endpoint}") String regionBEndpoint,
            MeterRegistry meterRegistry
    ) {
        this.webClient = WebClient.builder().build();
        this.regionEndpoints = Map.of(
                "region-a", regionAEndpoint,
                "region-b", regionBEndpoint
        );
        this.healthCache.put("region-a", true);
        this.healthCache.put("region-b", true);
        this.fallbackCounter = meterRegistry.counter("gateway.routing.fallbacks");
    }

    /**
     * Resolves the target URL for a given preferred region.
     * Falls back to the other region if the preferred one is unhealthy.
     *
     * @param preferredRegion region from the X-Region header (may be null)
     * @return the base URL of the target producer, or null if both are down
     */
    public String resolveTargetUrl(String preferredRegion) {
        String target = (preferredRegion != null && regionEndpoints.containsKey(preferredRegion))
                ? preferredRegion : "region-a";

        if (isHealthy(target)) {
            return regionEndpoints.get(target);
        }

        // Fallback to the other region
        fallbackCounter.increment();
        String fallback = target.equals("region-a") ? "region-b" : "region-a";
        log.warn("Region {} unhealthy, falling back to {}", target, fallback);

        if (isHealthy(fallback)) {
            return regionEndpoints.get(fallback);
        }

        log.error("Both regions are unhealthy!");
        return null; // Caller returns 503
    }

    /** Refresh health status for a single region (called by the health-check scheduler) */
    public void refreshHealth(String region) {
        String endpoint = regionEndpoints.get(region);
        if (endpoint == null) return;

        try {
            webClient.get()
                    .uri(endpoint + "/logs/health")
                    .retrieve()
                    .bodyToMono(java.util.Map.class)
                    .timeout(Duration.ofSeconds(2))
                    .subscribe(
                            resp -> healthCache.put(region, true),
                            err -> {
                                log.warn("Health check failed for {}: {}", region, err.getMessage());
                                healthCache.put(region, false);
                            }
                    );
        } catch (Exception e) {
            healthCache.put(region, false);
        }
    }

    public boolean isHealthy(String region) {
        return healthCache.getOrDefault(region, false);
    }

    public Map<String, Boolean> getAllHealthStatus() {
        return healthCache;
    }
}
