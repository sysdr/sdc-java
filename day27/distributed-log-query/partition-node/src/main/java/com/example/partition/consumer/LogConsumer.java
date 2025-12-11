package com.example.partition.consumer;

import com.example.partition.entity.LogEntryEntity;
import com.example.partition.repository.LogEntryRepository;
import com.example.partition.service.MetadataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogConsumer {
    
    private final LogEntryRepository repository;
    private final MetadataService metadataService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @KafkaListener(topics = "${kafka.topic:logs}", groupId = "${partition.id:partition-1}")
    public void consume(String message) {
        try {
            Map<String, Object> logData = objectMapper.readValue(message, Map.class);
            
            LogEntryEntity entity = LogEntryEntity.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.parse((String) logData.get("timestamp")))
                .logLevel((String) logData.get("level"))
                .serviceName((String) logData.get("serviceName"))
                .message((String) logData.get("message"))
                .traceId((String) logData.get("traceId"))
                .partitionId((String) logData.get("partitionId"))
                .build();
            
            repository.save(entity);
            
            if (repository.count() % 100 == 0) {
                metadataService.refreshMetadata();
            }
            
        } catch (Exception e) {
            log.error("Error consuming log message", e);
        }
    }
}
