package com.example.logprocessor.storage;

import com.example.logprocessor.common.LogEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {
    
    private final LogRepository logRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;
    
    public void storeLog(LogEvent event) {
        try {
            // Convert to entity
            LogEntity entity = LogEntity.builder()
                .id(event.getId())
                .message(event.getMessage())
                .level(event.getLevel())
                .source(event.getSource())
                .sourceIp(event.getSourceIp())
                .timestamp(event.getTimestamp())
                .application(event.getApplication())
                .environment(event.getEnvironment())
                .assignedNode(event.getAssignedNode())
                .hashValue(event.getHashValue())
                .build();
            
            // Store in PostgreSQL
            logRepository.save(entity);
            
            // Update log count in Redis
            String nodeId = System.getenv("NODE_ID");
            String countKey = "ring:logs:" + nodeId;
            redisTemplate.opsForValue().increment(countKey);
            
            // Metrics
            Counter.builder("storage.logs.stored")
                .tag("level", event.getLevel())
                .register(meterRegistry)
                .increment();
            
            log.debug("Stored log {} on node {}", event.getId(), nodeId);
            
        } catch (Exception e) {
            log.error("Failed to store log {}", event.getId(), e);
            meterRegistry.counter("storage.logs.failed").increment();
            throw e;
        }
    }
    
    public List<LogEvent> queryBySourceIp(String sourceIp) {
        return logRepository.findBySourceIp(sourceIp).stream()
            .map(this::toLogEvent)
            .collect(Collectors.toList());
    }
    
    public List<LogEvent> queryBySourceIpAndTimeRange(
            String sourceIp, Instant start, Instant end) {
        return logRepository.findBySourceIpAndTimestampBetween(sourceIp, start, end)
            .stream()
            .map(this::toLogEvent)
            .collect(Collectors.toList());
    }
    
    public long getLogCount() {
        return logRepository.countAllLogs();
    }
    
    private LogEvent toLogEvent(LogEntity entity) {
        return LogEvent.builder()
            .id(entity.getId())
            .message(entity.getMessage())
            .level(entity.getLevel())
            .source(entity.getSource())
            .sourceIp(entity.getSourceIp())
            .timestamp(entity.getTimestamp())
            .application(entity.getApplication())
            .environment(entity.getEnvironment())
            .assignedNode(entity.getAssignedNode())
            .hashValue(entity.getHashValue())
            .build();
    }
}
