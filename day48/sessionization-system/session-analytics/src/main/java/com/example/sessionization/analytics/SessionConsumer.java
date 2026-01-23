package com.example.sessionization.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SessionConsumer {
    private static final Logger log = LoggerFactory.getLogger(SessionConsumer.class);
    
    private final SessionRepository repository;
    private final ObjectMapper objectMapper;

    public SessionConsumer(SessionRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "completed-sessions", groupId = "session-analytics")
    public void consumeSession(String message) {
        try {
            SessionAggregate aggregate = objectMapper.readValue(message, SessionAggregate.class);
            
            SessionEntity entity = new SessionEntity();
            entity.setSessionId(aggregate.getSessionId());
            entity.setUserId(aggregate.getUserId());
            entity.setStartTime(Instant.ofEpochMilli(aggregate.getStartTime()));
            entity.setEndTime(Instant.ofEpochMilli(aggregate.getEndTime()));
            entity.setDurationSeconds(aggregate.getDurationSeconds());
            entity.setEventCount(aggregate.getEventCount());
            entity.setEventTypeCounts(aggregate.getEventTypeCounts());
            entity.setPagesVisited(aggregate.getPagesVisited());
            entity.setHasConversion(aggregate.isHasConversion());
            entity.setDeviceType(aggregate.getDeviceType());
            entity.setLocation(aggregate.getLocation());
            
            repository.save(entity);
            
            log.info("Persisted session: userId={}, duration={}s, events={}, converted={}",
                entity.getUserId(), entity.getDurationSeconds(), 
                entity.getEventCount(), entity.isHasConversion());
                
        } catch (Exception e) {
            log.error("Error processing session message: {}", message, e);
        }
    }
}
