package com.example.logprocessor.producer.generator;

import com.example.logprocessor.common.format.LogFormat;
import com.example.logprocessor.common.proto.LogEventProtos;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class MultiFormatLogGenerator {

    private final ObjectMapper objectMapper;
    private final Schema avroSchema;
    private final GenericDatumWriter<GenericRecord> avroWriter;

    private static final String[] SERVICES = {
        "auth-service", "payment-service", "order-service", 
        "inventory-service", "notification-service"
    };
    private static final String[] LEVELS = {"DEBUG", "INFO", "WARN", "ERROR"};
    private static final String[] MESSAGES = {
        "Request processed successfully",
        "Database connection established",
        "Cache miss, fetching from database",
        "Rate limit exceeded for user",
        "Payment transaction completed",
        "Order placed successfully",
        "Inventory updated",
        "Email notification sent",
        "Authentication successful",
        "Session expired"
    };

    public MultiFormatLogGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        
        String schemaJson = """
            {
              "type": "record",
              "name": "LogEvent",
              "namespace": "com.example.logprocessor",
              "fields": [
                {"name": "id", "type": "string"},
                {"name": "timestamp", "type": "long"},
                {"name": "level", "type": "string"},
                {"name": "service", "type": "string"},
                {"name": "host", "type": "string"},
                {"name": "message", "type": "string"},
                {"name": "traceId", "type": ["null", "string"], "default": null},
                {"name": "spanId", "type": ["null", "string"], "default": null},
                {"name": "metadata", "type": {"type": "map", "values": "string"}, "default": {}}
              ]
            }
            """;
        this.avroSchema = new Schema.Parser().parse(schemaJson);
        this.avroWriter = new GenericDatumWriter<>(avroSchema);
    }

    public byte[] generate(LogFormat format) {
        return switch (format) {
            case TEXT -> generateText();
            case JSON -> generateJson();
            case PROTOBUF -> generateProtobuf();
            case AVRO -> generateAvro();
        };
    }

    public byte[] generateText() {
        String timestamp = Instant.now().toString();
        String level = randomElement(LEVELS);
        String service = randomElement(SERVICES);
        String message = randomElement(MESSAGES);

        String log = String.format("%s [%s] [%s] %s requestId=%s duration=%dms",
                timestamp, level, service, message,
                UUID.randomUUID().toString().substring(0, 8),
                ThreadLocalRandom.current().nextInt(10, 500));

        return log.getBytes();
    }

    public byte[] generateJson() {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", UUID.randomUUID().toString());
            node.put("timestamp", Instant.now().toEpochMilli());
            node.put("level", randomElement(LEVELS));
            node.put("service", randomElement(SERVICES));
            node.put("host", "host-" + ThreadLocalRandom.current().nextInt(1, 10));
            node.put("message", randomElement(MESSAGES));
            node.put("traceId", UUID.randomUUID().toString());
            node.put("spanId", UUID.randomUUID().toString().substring(0, 16));

            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.put("requestId", UUID.randomUUID().toString().substring(0, 8));
            metadata.put("duration", ThreadLocalRandom.current().nextInt(10, 500));
            node.set("metadata", metadata);

            return objectMapper.writeValueAsBytes(node);
        } catch (Exception e) {
            log.error("Failed to generate JSON log", e);
            return "{}".getBytes();
        }
    }

    public byte[] generateProtobuf() {
        try {
            LogEventProtos.LogEvent event = LogEventProtos.LogEvent.newBuilder()
                    .setId(UUID.randomUUID().toString())
                    .setTimestamp(Instant.now().toEpochMilli())
                    .setLevel(randomElement(LEVELS))
                    .setService(randomElement(SERVICES))
                    .setHost("host-" + ThreadLocalRandom.current().nextInt(1, 10))
                    .setMessage(randomElement(MESSAGES))
                    .setTraceId(UUID.randomUUID().toString())
                    .setSpanId(UUID.randomUUID().toString().substring(0, 16))
                    .putMetadata("requestId", UUID.randomUUID().toString().substring(0, 8))
                    .putMetadata("duration", String.valueOf(
                            ThreadLocalRandom.current().nextInt(10, 500)))
                    .build();

            return event.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate Protobuf log", e);
            return new byte[0];
        }
    }

    public byte[] generateAvro() {
        try {
            GenericRecord record = new GenericData.Record(avroSchema);
            record.put("id", UUID.randomUUID().toString());
            record.put("timestamp", Instant.now().toEpochMilli());
            record.put("level", randomElement(LEVELS));
            record.put("service", randomElement(SERVICES));
            record.put("host", "host-" + ThreadLocalRandom.current().nextInt(1, 10));
            record.put("message", randomElement(MESSAGES));
            record.put("traceId", UUID.randomUUID().toString());
            record.put("spanId", UUID.randomUUID().toString().substring(0, 16));

            Map<String, String> metadata = new HashMap<>();
            metadata.put("requestId", UUID.randomUUID().toString().substring(0, 8));
            metadata.put("duration", String.valueOf(
                    ThreadLocalRandom.current().nextInt(10, 500)));
            record.put("metadata", metadata);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(baos, null);
            avroWriter.write(record, encoder);
            encoder.flush();

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate Avro log", e);
            return new byte[0];
        }
    }

    private String randomElement(String[] array) {
        return array[ThreadLocalRandom.current().nextInt(array.length)];
    }
}
