package com.example.logprocessor.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class MetricsConfig {
    
    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    private final AtomicLong currentEpoch = new AtomicLong(0);
    private final AtomicLong lastFailoverTime = new AtomicLong(0);
    
    @Bean
    public Counter failoverEventsCounter(MeterRegistry registry) {
        return Counter.builder("failover.events.total")
            .description("Total number of failover events")
            .register(registry);
    }
    
    @Bean
    public Timer failoverRecoveryTimer(MeterRegistry registry) {
        return Timer.builder("failover.recovery_time_seconds")
            .description("Time taken to recover from failover")
            .register(registry);
    }
    
    @Bean
    public Counter messagesProcessedCounter(MeterRegistry registry) {
        return Counter.builder("messages.processed.total")
            .description("Total messages processed")
            .register(registry);
    }
    
    @Bean
    public Counter messagesLostCounter(MeterRegistry registry) {
        return Counter.builder("messages.lost.total")
            .description("Messages lost during failover")
            .register(registry);
    }
    
    @Bean
    public Gauge leaderGauge(MeterRegistry registry) {
        return Gauge.builder("leader.status", isLeader, b -> b.get() ? 1.0 : 0.0)
            .description("Whether this instance is the leader (1) or standby (0)")
            .register(registry);
    }
    
    @Bean
    public Gauge epochGauge(MeterRegistry registry) {
        return Gauge.builder("leader.epoch", currentEpoch, AtomicLong::get)
            .description("Current leadership epoch")
            .register(registry);
    }
    
    public void setLeaderStatus(boolean leader) {
        isLeader.set(leader);
    }
    
    public void setEpoch(long epoch) {
        currentEpoch.set(epoch);
    }
    
    public void recordFailover() {
        lastFailoverTime.set(System.currentTimeMillis());
    }
}
