package com.example.logprocessor.gateway.config;

import com.example.logprocessor.gateway.service.RegionRoutingService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Periodic health poller for all registered regions.
 * Runs every 5 seconds. The interval is intentionally short:
 * in a multi-region setup, a 5s stale health cache means at most
 * 5s of requests hitting a dead region before failover kicks in.
 */
@Configuration
@EnableScheduling
public class HealthCheckScheduler {

    private final RegionRoutingService routingService;

    public HealthCheckScheduler(RegionRoutingService routingService) {
        this.routingService = routingService;
    }

    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void pollRegionHealth() {
        routingService.refreshHealth("region-a");
        routingService.refreshHealth("region-b");
    }
}
