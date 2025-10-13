package com.example.logprocessor.producer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String TOPIC = "compressed-logs";

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final CompressionService compressionService;
    private final CompressionMetrics metrics;

    public KafkaProducerService(KafkaTemplate<String, byte[]> kafkaTemplate,
                                CompressionService compressionService,
                                CompressionMetrics metrics) {
        this.kafkaTemplate = kafkaTemplate;
        this.compressionService = compressionService;
        this.metrics = metrics;
    }

    public void sendLog(String logData) {
        try {
            // Select compression algorithm
            CompressionAlgorithm algorithm = compressionService.selectAlgorithm(logData);
            
            // Compress with telemetry
            long startTime = System.nanoTime();
            byte[] compressed = compressionService.compress(logData, algorithm);
            long compressionTime = System.nanoTime() - startTime;
            
            // Calculate metrics
            long originalSize = logData.getBytes(StandardCharsets.UTF_8).length;
            metrics.recordCompression(originalSize, compressed.length, 
                compressionTime, algorithm.getName());
            
            // Create record with compression metadata
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(TOPIC, compressed);
            record.headers().add(new RecordHeader("compression-algorithm", 
                algorithm.getName().getBytes(StandardCharsets.UTF_8)));
            record.headers().add(new RecordHeader("original-size", 
                String.valueOf(originalSize).getBytes(StandardCharsets.UTF_8)));
            
            // Send to Kafka
            kafkaTemplate.send(record).whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.debug("Log sent successfully with {} compression. Original: {} bytes, Compressed: {} bytes", 
                        algorithm.getName(), originalSize, compressed.length);
                } else {
                    logger.error("Failed to send log", ex);
                }
            });
            
        } catch (Exception e) {
            logger.error("Error sending log", e);
        }
    }
}
