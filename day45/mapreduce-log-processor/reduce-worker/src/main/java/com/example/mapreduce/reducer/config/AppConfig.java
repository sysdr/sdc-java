package com.example.mapreduce.reducer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
@EnableScheduling
public class AppConfig {
    
    private final AtomicLong lastHeartbeat = new AtomicLong(System.currentTimeMillis());
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
    
    @Bean
    public String registerHeartbeatMetric(MeterRegistry meterRegistry) {
        Gauge.builder("mapreduce_worker_heartbeat_lag_seconds", lastHeartbeat, 
            heartbeat -> (System.currentTimeMillis() - heartbeat.get()) / 1000.0)
            .description("Worker heartbeat lag in seconds")
            .tag("worker_type", "reduce")
            .register(meterRegistry);
        return "heartbeat-metric-registered";
    }
    
    @Scheduled(fixedRate = 5000)
    public void sendHeartbeat() {
        lastHeartbeat.set(System.currentTimeMillis());
    }
}
