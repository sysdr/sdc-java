package com.example.cluster.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Connection;

@Service
public class HealthScoreService {
    private static final Logger logger = LoggerFactory.getLogger(HealthScoreService.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    // Weights for different health components
    private static final double JVM_WEIGHT = 0.30;
    private static final double KAFKA_WEIGHT = 0.30;
    private static final double REDIS_WEIGHT = 0.20;
    private static final double SYSTEM_WEIGHT = 0.20;
    
    public HealthScoreService(RedisTemplate<String, String> redisTemplate,
                              KafkaTemplate<String, String> kafkaTemplate) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public int getCurrentScore() {
        try {
            int jvmScore = checkJvmHealth();
            int kafkaScore = checkKafkaHealth();
            int redisScore = checkRedisHealth();
            int systemScore = checkSystemHealth();
            
            double weightedScore = 
                (jvmScore * JVM_WEIGHT) +
                (kafkaScore * KAFKA_WEIGHT) +
                (redisScore * REDIS_WEIGHT) +
                (systemScore * SYSTEM_WEIGHT);
            
            return (int) Math.round(weightedScore);
            
        } catch (Exception e) {
            logger.error("Error calculating health score", e);
            return 0;
        }
    }
    
    private int checkJvmHealth() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long used = memoryBean.getHeapMemoryUsage().getUsed();
            long max = memoryBean.getHeapMemoryUsage().getMax();
            double usage = (double) used / max;
            
            if (usage < 0.70) return 100;
            if (usage < 0.80) return 80;
            if (usage < 0.90) return 50;
            return 20;
            
        } catch (Exception e) {
            return 50;
        }
    }
    
    private int checkKafkaHealth() {
        try {
            // Simple connectivity check - if we can get metadata, Kafka is healthy
            kafkaTemplate.getDefaultTopic();
            return 100;
        } catch (Exception e) {
            logger.warn("Kafka health check failed", e);
            return 0;
        }
    }
    
    private int checkRedisHealth() {
        try {
            long start = System.nanoTime();
            redisTemplate.opsForValue().get("health:check");
            long latencyNanos = System.nanoTime() - start;
            double latencyMs = latencyNanos / 1_000_000.0;
            
            if (latencyMs < 5) return 100;
            if (latencyMs < 10) return 80;
            if (latencyMs < 20) return 50;
            return 20;
            
        } catch (Exception e) {
            logger.warn("Redis health check failed", e);
            return 0;
        }
    }
    
    private int checkSystemHealth() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getSystemLoadAverage();
            int processors = osBean.getAvailableProcessors();
            
            // Normalize load by processor count
            double normalizedLoad = cpuLoad / processors;
            
            if (normalizedLoad < 0.5) return 100;
            if (normalizedLoad < 0.7) return 80;
            if (normalizedLoad < 0.9) return 50;
            return 20;
            
        } catch (Exception e) {
            return 50;
        }
    }
}
