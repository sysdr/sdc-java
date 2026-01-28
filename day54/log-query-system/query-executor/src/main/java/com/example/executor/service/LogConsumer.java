package com.example.executor.service;

import com.example.executor.model.LogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
public class LogConsumer {
    
    @Autowired
    private EntityManager entityManager;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @KafkaListener(topics = "log-events", groupId = "query-executor")
    @Transactional
    public void consume(String message) {
        try {
            Map<String, Object> logData = objectMapper.readValue(message, Map.class);
            
            LogEntry entry = new LogEntry();
            entry.setTimestamp(Instant.now());
            entry.setLevel((String) logData.get("level"));
            entry.setService((String) logData.get("service"));
            entry.setMessage((String) logData.get("message"));
            entry.setTraceId((String) logData.get("traceId"));
            entry.setPartitionId((Integer) logData.get("partition"));
            
            entityManager.persist(entry);
            
            if (log.isDebugEnabled()) {
                log.debug("Persisted log entry: {}", entry.getId());
            }
            
        } catch (Exception e) {
            log.error("Failed to process log message", e);
        }
    }
}
