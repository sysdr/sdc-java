package com.example.logprocessor.normalizer.handler;

import com.example.logprocessor.common.format.LogFormat;
import com.example.logprocessor.common.model.CanonicalLog;
import com.example.logprocessor.common.proto.LogEventProtos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class ProtobufFormatHandler implements FormatHandler {

    @Override
    public LogFormat getFormat() {
        return LogFormat.PROTOBUF;
    }

    @Override
    public CanonicalLog parse(byte[] input) throws FormatParseException {
        try {
            LogEventProtos.LogEvent protoEvent = LogEventProtos.LogEvent.parseFrom(input);

            Map<String, String> metadata = new HashMap<>(protoEvent.getMetadataMap());

            return CanonicalLog.builder()
                    .id(protoEvent.getId().isEmpty() ? UUID.randomUUID().toString() : protoEvent.getId())
                    .timestamp(Instant.ofEpochMilli(protoEvent.getTimestamp()))
                    .level(CanonicalLog.LogLevel.fromString(protoEvent.getLevel()))
                    .service(protoEvent.getService())
                    .host(protoEvent.getHost())
                    .message(protoEvent.getMessage())
                    .traceId(protoEvent.getTraceId().isEmpty() ? null : protoEvent.getTraceId())
                    .spanId(protoEvent.getSpanId().isEmpty() ? null : protoEvent.getSpanId())
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            throw new FormatParseException("Failed to parse Protobuf: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] serialize(CanonicalLog log) throws FormatSerializeException {
        try {
            LogEventProtos.LogEvent.Builder builder = LogEventProtos.LogEvent.newBuilder()
                    .setId(log.getId())
                    .setTimestamp(log.getTimestamp().toEpochMilli())
                    .setLevel(log.getLevel().name())
                    .setService(log.getService())
                    .setHost(log.getHost())
                    .setMessage(log.getMessage());

            if (log.getTraceId() != null) {
                builder.setTraceId(log.getTraceId());
            }
            if (log.getSpanId() != null) {
                builder.setSpanId(log.getSpanId());
            }
            if (log.getMetadata() != null) {
                builder.putAllMetadata(log.getMetadata());
            }

            return builder.build().toByteArray();

        } catch (Exception e) {
            throw new FormatSerializeException("Failed to serialize to Protobuf: " + e.getMessage(), e);
        }
    }
}
