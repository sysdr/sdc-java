package com.example.logprocessor.consumer;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final DecompressionService decompressionService;
    private final DecompressionMetrics metrics;
    private final LogRepository logRepository;
    private final CircuitBreaker circuitBreaker;

    public KafkaConsumerService(DecompressionService decompressionService,
                                DecompressionMetrics metrics,
                                LogRepository logRepository,
                                CircuitBreakerRegistry circuitBreakerRegistry) {
        this.decompressionService = decompressionService;
        this.metrics = metrics;
        this.logRepository = logRepository;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("decompression");
    }

    @KafkaListener(topics = "compressed-logs", groupId = "log-consumer-group")
    public void consume(ConsumerRecord<String, byte[]> record) {
        String algorithm = extractAlgorithm(record);
        
        try {
            long startTime = System.nanoTime();
            
            String decompressed = circuitBreaker.executeSupplier(() -> {
                try {
                    return decompressionService.decompress(record.value(), algorithm);
                } catch (Exception e) {
                    throw new RuntimeException("Decompression failed", e);
                }
            });
            
            long decompressTime = System.nanoTime() - startTime;
            metrics.recordDecompression(decompressTime, algorithm, true);
            
            processLog(decompressed);
            
        } catch (Exception e) {
            logger.error("Failed to process log with algorithm {}", algorithm, e);
            metrics.recordDecompression(0, algorithm, false);
            
            if (circuitBreaker.getState().toString().equals("OPEN")) {
                metrics.recordCircuitBreakerOpen(algorithm);
            }
        }
    }

    private String extractAlgorithm(ConsumerRecord<String, byte[]> record) {
        Header header = record.headers().lastHeader("compression-algorithm");
        if (header != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return "none";
    }

    private void processLog(String logJson) {
        try {
            LogEntry entry = parseLog(logJson);
            logRepository.save(entry);
            logger.debug("Saved log: {}", entry.getMessage());
        } catch (Exception e) {
            logger.error("Failed to save log", e);
        }
    }

    private LogEntry parseLog(String json) {
        LogEntry entry = new LogEntry();
        entry.setTimestamp(Instant.now());
        entry.setLevel(extractField(json, "level"));
        entry.setService(extractField(json, "service"));
        entry.setMessage(extractField(json, "message"));
        entry.setMetadata(json);
        return entry;
    }

    private String extractField(String json, String field) {
        String searchKey = "\"" + field + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return "unknown";
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "unknown";
        return json.substring(start, end);
    }
}
