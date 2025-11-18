package com.example.logprocessor.producer.service;

import com.example.logprocessor.producer.model.LogEventRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class AvroProducerService {

    private static final Logger logger = LoggerFactory.getLogger(AvroProducerService.class);

    private final KafkaTemplate<String, GenericRecord> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    
    private Schema schemaV1;
    private Schema schemaV2;
    
    private Counter eventsProducedCounter;
    private Counter productionErrorCounter;
    private Timer productionTimer;

    @Value("${kafka.topic.logs}")
    private String logsTopic;

    public AvroProducerService(
            KafkaTemplate<String, GenericRecord> kafkaTemplate,
            MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() throws IOException {
        // Load schemas from classpath
        schemaV1 = loadSchema("/avro/LogEventV1.avsc");
        schemaV2 = loadSchema("/avro/LogEventV2.avsc");
        
        // Initialize metrics
        eventsProducedCounter = Counter.builder("avro.events.produced")
            .description("Number of Avro events produced")
            .register(meterRegistry);
            
        productionErrorCounter = Counter.builder("avro.events.errors")
            .description("Number of production errors")
            .register(meterRegistry);
            
        productionTimer = Timer.builder("avro.production.time")
            .description("Time to produce Avro events")
            .register(meterRegistry);
            
        logger.info("Loaded Avro schemas - V1: {}, V2: {}", 
            schemaV1.getFullName(), schemaV2.getFullName());
    }

    private Schema loadSchema(String path) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Schema not found: " + path);
            }
            return new Schema.Parser().parse(is);
        }
    }

    @CircuitBreaker(name = "kafkaProducer", fallbackMethod = "produceFallback")
    public CompletableFuture<SendResult<String, GenericRecord>> produceLogEvent(
            LogEventRequest request) {
        
        return productionTimer.record(() -> {
            String eventId = UUID.randomUUID().toString();
            
            GenericRecord record;
            if (request.schemaVersion() == 1) {
                record = createV1Record(eventId, request);
            } else {
                record = createV2Record(eventId, request);
            }
            
            CompletableFuture<SendResult<String, GenericRecord>> future = 
                kafkaTemplate.send(logsTopic, eventId, record);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    eventsProducedCounter.increment();
                    logger.debug("Produced event {} to partition {} at offset {}",
                        eventId,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    productionErrorCounter.increment();
                    logger.error("Failed to produce event {}: {}", eventId, ex.getMessage());
                }
            });
            
            return future;
        });
    }

    private GenericRecord createV1Record(String eventId, LogEventRequest request) {
        GenericRecord record = new GenericData.Record(schemaV1);
        record.put("id", eventId);
        record.put("timestamp", System.currentTimeMillis());
        record.put("level", new GenericData.EnumSymbol(
            schemaV1.getField("level").schema(), request.level()));
        record.put("message", request.message());
        record.put("source", request.source());
        record.put("schemaVersion", 1);
        return record;
    }

    private GenericRecord createV2Record(String eventId, LogEventRequest request) {
        GenericRecord record = new GenericData.Record(schemaV2);
        record.put("id", eventId);
        record.put("timestamp", System.currentTimeMillis());
        record.put("level", new GenericData.EnumSymbol(
            schemaV2.getField("level").schema(), request.level()));
        record.put("message", request.message());
        record.put("source", request.source());
        record.put("schemaVersion", 2);
        record.put("correlationId", request.correlationId());
        record.put("tags", request.tags() != null ? request.tags() : new java.util.HashMap<>());
        record.put("spanId", request.spanId());
        record.put("parentSpanId", request.parentSpanId());
        return record;
    }

    public CompletableFuture<SendResult<String, GenericRecord>> produceFallback(
            LogEventRequest request, Throwable t) {
        logger.error("Circuit breaker triggered for Kafka producer: {}", t.getMessage());
        productionErrorCounter.increment();
        return CompletableFuture.failedFuture(t);
    }

    public Schema getSchemaV1() {
        return schemaV1;
    }

    public Schema getSchemaV2() {
        return schemaV2;
    }
}
