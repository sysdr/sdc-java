package com.example.gateway;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class BackpressureConfig {

    @Bean
    public RateLimiter apiRateLimiter(MeterRegistry meterRegistry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(1000)  // 1000 requests per second initially
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofMillis(100))
            .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        RateLimiter rateLimiter = registry.rateLimiter("api-gateway");

        // Register metrics
        meterRegistry.gauge("rate_limiter.available_permissions", rateLimiter,
            rl -> rl.getMetrics().getAvailablePermissions());

        return rateLimiter;
    }
}
