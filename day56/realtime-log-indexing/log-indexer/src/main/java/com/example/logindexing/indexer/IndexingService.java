package com.example.logindexing.indexer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
public class IndexingService {

    private final InvertedIndex invertedIndex;
    private final LogDocumentRepository documentRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Timer indexingLatency;
    private final Counter documentsIndexed;
    private final Counter indexingErrors;

    public IndexingService(InvertedIndex invertedIndex,
                          LogDocumentRepository documentRepository,
                          RedisTemplate<String, String> redisTemplate,
                          ObjectMapper objectMapper,
                          MeterRegistry meterRegistry) {
        this.invertedIndex = invertedIndex;
        this.documentRepository = documentRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.indexingLatency = Timer.builder("indexing.latency")
                .description("Time to index a document")
                .register(meterRegistry);
        this.documentsIndexed = Counter.builder("documents.indexed.total")
                .description("Total documents indexed")
                .register(meterRegistry);
        this.indexingErrors = Counter.builder("indexing.errors.total")
                .description("Total indexing errors")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "raw-logs", groupId = "log-indexer-group", concurrency = "3")
    public void consumeAndIndex(String message) {
        Instant startTime = Instant.now();
        
        try {
            // Parse log event
            Map<String, Object> logEvent = objectMapper.readValue(message, Map.class);
            String docId = (String) logEvent.get("id");
            
            indexingLatency.record(() -> {
                // Build searchable text
                String searchableText = buildSearchableText(logEvent);
                
                // Add to inverted index (in-memory)
                invertedIndex.addDocument(docId, searchableText);
                
                // Store in PostgreSQL for document retrieval
                storeDocument(logEvent);
                
                // Cache in Redis for fast lookup
                cacheDocument(docId, message);
            });
            
            documentsIndexed.increment();
            
            long latencyMs = Duration.between(startTime, Instant.now()).toMillis();
            if (latencyMs > 100) {
                log.warn("High indexing latency: {}ms for document {}", latencyMs, docId);
            }
            
        } catch (Exception e) {
            indexingErrors.increment();
            log.error("Failed to index document", e);
        }
    }

    private String buildSearchableText(Map<String, Object> logEvent) {
        StringBuilder sb = new StringBuilder();
        sb.append(logEvent.get("level")).append(" ");
        sb.append(logEvent.get("service")).append(" ");
        sb.append(logEvent.get("message")).append(" ");
        if (logEvent.get("userId") != null) {
            sb.append(logEvent.get("userId")).append(" ");
        }
        return sb.toString();
    }

    @Transactional
    private void storeDocument(Map<String, Object> logEvent) {
        try {
            LogDocument document = LogDocument.builder()
                    .id((String) logEvent.get("id"))
                    .timestamp(Instant.parse((String) logEvent.get("timestamp")))
                    .level((String) logEvent.get("level"))
                    .service((String) logEvent.get("service"))
                    .message((String) logEvent.get("message"))
                    .userId((String) logEvent.get("userId"))
                    .traceId((String) logEvent.get("traceId"))
                    .metadataJson(objectMapper.writeValueAsString(logEvent.get("metadata")))
                    .indexedAt(Instant.now())
                    .build();
            
            documentRepository.save(document);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metadata", e);
        }
    }

    private void cacheDocument(String docId, String document) {
        try {
            redisTemplate.opsForValue().set(
                "doc:" + docId, 
                document, 
                Duration.ofHours(1)
            );
        } catch (Exception e) {
            log.warn("Failed to cache document {}", docId, e);
        }
    }
}
