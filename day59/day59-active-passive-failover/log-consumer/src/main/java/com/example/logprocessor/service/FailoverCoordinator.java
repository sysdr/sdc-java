package com.example.logprocessor.service;

import com.example.logprocessor.config.MetricsConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class FailoverCoordinator implements LeaderLatchListener {
    
    private final CuratorFramework curatorFramework;
    private final StatefulLogConsumer logConsumer;
    private final StateManager stateManager;
    private final RedisTemplate<String, String> redisTemplate;
    private final MetricsConfig metricsConfig;
    private final Counter failoverCounter;
    private final Timer recoveryTimer;
    
    private LeaderLatch leaderLatch;
    private final String instanceId;
    private long currentEpoch = 0;
    private Instant failoverStartTime;
    
    @Value("${failover.leader-path:/failover/leader}")
    private String leaderPath;
    
    public FailoverCoordinator(
            CuratorFramework curatorFramework,
            StatefulLogConsumer logConsumer,
            StateManager stateManager,
            RedisTemplate<String, String> redisTemplate,
            MetricsConfig metricsConfig,
            Counter failoverEventsCounter,
            Timer failoverRecoveryTimer) {
        this.curatorFramework = curatorFramework;
        this.logConsumer = logConsumer;
        this.stateManager = stateManager;
        this.redisTemplate = redisTemplate;
        this.metricsConfig = metricsConfig;
        this.failoverCounter = failoverEventsCounter;
        this.recoveryTimer = failoverRecoveryTimer;
        this.instanceId = UUID.randomUUID().toString();
    }
    
    @PostConstruct
    public void init() throws Exception {
        log.info("Initializing FailoverCoordinator for instance: {}", instanceId);
        leaderLatch = new LeaderLatch(curatorFramework, leaderPath, instanceId);
        leaderLatch.addListener(this);
        leaderLatch.start();
    }
    
    @PreDestroy
    public void shutdown() throws Exception {
        log.info("Shutting down FailoverCoordinator");
        if (leaderLatch != null) {
            leaderLatch.close();
        }
    }
    
    @Override
    public void isLeader() {
        failoverStartTime = Instant.now();
        currentEpoch++;
        
        log.info("🎯 Instance {} became LEADER with epoch {}", instanceId, currentEpoch);
        
        try {
            // Step 1: Update leadership status in Redis
            redisTemplate.opsForValue().set("leader:instance", instanceId);
            redisTemplate.opsForValue().set("leader:epoch", String.valueOf(currentEpoch));
            
            // Step 2: Validate state consistency
            boolean stateValid = stateManager.validateStateConsistency();
            if (!stateValid) {
                log.error("State validation failed! Attempting recovery...");
                stateManager.recoverState();
            }
            
            // Step 3: Grace period to prevent split-brain
            log.info("Entering 2-second grace period...");
            Thread.sleep(2000);
            
            // Step 4: Resume Kafka consumption
            log.info("Resuming Kafka consumption as leader");
            logConsumer.setInstanceId(instanceId);
            logConsumer.resumeAsLeader(currentEpoch);
            
            // Step 5: Update metrics
            metricsConfig.setLeaderStatus(true);
            metricsConfig.setEpoch(currentEpoch);
            failoverCounter.increment();
            metricsConfig.recordFailover();
            
            // Record recovery time
            Duration recoveryTime = Duration.between(failoverStartTime, Instant.now());
            recoveryTimer.record(recoveryTime);
            
            log.info("✅ Failover complete! Recovery time: {}ms", recoveryTime.toMillis());
            
        } catch (Exception e) {
            log.error("Failed to assume leadership, releasing lock", e);
            try {
                leaderLatch.close();
            } catch (Exception ex) {
                log.error("Failed to release leader latch", ex);
            }
        }
    }
    
    @Override
    public void notLeader() {
        log.info("📊 Instance {} is now STANDBY", instanceId);
        
        // Stop Kafka consumption
        logConsumer.pauseAsStandby();
        
        // Update metrics
        metricsConfig.setLeaderStatus(false);
        
        log.info("Transitioned to standby mode successfully");
    }
    
    public String getInstanceId() {
        return instanceId;
    }
    
    public boolean hasLeadership() {
        return leaderLatch.hasLeadership();
    }
    
    public long getCurrentEpoch() {
        return currentEpoch;
    }
}
