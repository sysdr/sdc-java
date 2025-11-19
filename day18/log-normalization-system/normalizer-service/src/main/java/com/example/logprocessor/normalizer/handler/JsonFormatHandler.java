package com.example.logprocessor.normalizer.handler;

import com.example.logprocessor.common.format.LogFormat;
import com.example.logprocessor.common.model.CanonicalLog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class JsonFormatHandler implements FormatHandler {

    private final ObjectMapper objectMapper;

    public JsonFormatHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public LogFormat getFormat() {
        return LogFormat.JSON;
    }

    @Override
    public CanonicalLog parse(byte[] input) throws FormatParseException {
        try {
            JsonNode root = objectMapper.readTree(input);

            Map<String, String> metadata = new HashMap<>();
            Iterator<String> fieldNames = root.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                if (!isReservedField(field)) {
                    JsonNode value = root.get(field);
                    if (value.isValueNode()) {
                        metadata.put(field, value.asText());
                    }
                }
            }

            return CanonicalLog.builder()
                    .id(getStringField(root, "id", UUID.randomUUID().toString()))
                    .timestamp(parseTimestamp(root))
                    .level(CanonicalLog.LogLevel.fromString(getStringField(root, "level", "INFO")))
                    .service(getStringField(root, "service", "unknown"))
                    .host(getStringField(root, "host", "unknown"))
                    .message(getStringField(root, "message", ""))
                    .traceId(getStringField(root, "traceId", null))
                    .spanId(getStringField(root, "spanId", null))
                    .metadata(metadata)
                    .rawContent(new String(input))
                    .build();

        } catch (Exception e) {
            throw new FormatParseException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] serialize(CanonicalLog log) throws FormatSerializeException {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("id", log.getId());
            root.put("timestamp", log.getTimestamp().toEpochMilli());
            root.put("level", log.getLevel().name());
            root.put("service", log.getService());
            root.put("host", log.getHost());
            root.put("message", log.getMessage());

            if (log.getTraceId() != null) {
                root.put("traceId", log.getTraceId());
            }
            if (log.getSpanId() != null) {
                root.put("spanId", log.getSpanId());
            }

            if (log.getMetadata() != null && !log.getMetadata().isEmpty()) {
                ObjectNode metadataNode = objectMapper.createObjectNode();
                log.getMetadata().forEach(metadataNode::put);
                root.set("metadata", metadataNode);
            }

            return objectMapper.writeValueAsBytes(root);

        } catch (Exception e) {
            throw new FormatSerializeException("Failed to serialize to JSON: " + e.getMessage(), e);
        }
    }

    private String getStringField(JsonNode node, String field, String defaultValue) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : defaultValue;
    }

    private Instant parseTimestamp(JsonNode root) {
        JsonNode tsNode = root.get("timestamp");
        if (tsNode == null) {
            return Instant.now();
        }
        if (tsNode.isNumber()) {
            return Instant.ofEpochMilli(tsNode.asLong());
        }
        try {
            return Instant.parse(tsNode.asText());
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private boolean isReservedField(String field) {
        return field.equals("id") || field.equals("timestamp") || field.equals("level") ||
               field.equals("service") || field.equals("host") || field.equals("message") ||
               field.equals("traceId") || field.equals("spanId") || field.equals("metadata");
    }
}
