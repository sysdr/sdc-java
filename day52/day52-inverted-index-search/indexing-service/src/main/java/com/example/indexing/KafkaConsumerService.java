package com.example.indexing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class KafkaConsumerService {
    
    private final InvertedIndex invertedIndex;
    private final LogDocumentRepository documentRepository;
    private final ObjectMapper objectMapper;
    private final Counter processedCounter;
    
    public KafkaConsumerService(InvertedIndex invertedIndex,
                               LogDocumentRepository documentRepository,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry) {
        this.invertedIndex = invertedIndex;
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper;
        this.processedCounter = Counter.builder("indexing_processed_logs")
            .description("Number of logs processed for indexing")
            .register(meterRegistry);
    }
    
    @KafkaListener(topics = "log-events", groupId = "indexing-service")
    public void consumeLogEvent(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            Long id = json.get("id").asLong();
            String level = json.get("level").asText();
            String service = json.get("service").asText();
            String logMessage = json.get("message").asText();
            String timestamp = json.get("timestamp").asText();
            String userId = json.get("userId").asText();
            String traceId = json.get("traceId").asText();
            String searchableText = String.format("%s %s %s %s", 
                level, service, logMessage, userId);
            invertedIndex.addDocument(id, searchableText);
            LogDocument document = new LogDocument();
            document.setId(id);
            document.setLevel(level);
            document.setService(service);
            document.setMessage(logMessage);
            document.setTimestamp(Instant.parse(timestamp));
            document.setUserId(userId);
            document.setTraceId(traceId);
            document.setIndexedAt(Instant.now());
            documentRepository.save(document);
            processedCounter.increment();
            if (id % 500 == 0) {
                log.info("Processed log event: {} - Index size: {} terms", 
                    id, invertedIndex.getIndexSize());
            }
        } catch (Exception e) {
            log.error("Error processing log event", e);
        }
    }
}
