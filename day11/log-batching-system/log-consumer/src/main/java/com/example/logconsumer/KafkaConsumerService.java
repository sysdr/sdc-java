package com.example.logconsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class KafkaConsumerService {
    
    private final LogRepository logRepository;
    private final ObjectMapper objectMapper;
    private final Counter logsProcessed;
    
    public KafkaConsumerService(LogRepository logRepository, 
                               ObjectMapper objectMapper,
                               MeterRegistry registry) {
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
        this.logsProcessed = Counter.builder("logs.processed")
            .description("Number of logs processed from Kafka")
            .register(registry);
    }
    
    @KafkaListener(topics = "${kafka.topic}", groupId = "log-consumer-group")
    public void consumeLogBatch(String batchJson) {
        try {
            LogEntry[] batch = objectMapper.readValue(batchJson, LogEntry[].class);
            List<LogEntry> entries = Arrays.asList(batch);
            
            logRepository.saveAll(entries);
            logsProcessed.increment(entries.size());
            
            log.info("Processed batch of {} logs", entries.size());
            
        } catch (Exception e) {
            log.error("Failed to process log batch", e);
        }
    }
}
