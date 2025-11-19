package com.example.logprocessor.normalizer.handler;

import com.example.logprocessor.common.format.LogFormat;
import com.example.logprocessor.common.model.CanonicalLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class AvroFormatHandler implements FormatHandler {

    private final Schema schema;
    private final GenericDatumWriter<GenericRecord> writer;
    private final GenericDatumReader<GenericRecord> reader;

    public AvroFormatHandler() {
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
        this.schema = new Schema.Parser().parse(schemaJson);
        this.writer = new GenericDatumWriter<>(schema);
        this.reader = new GenericDatumReader<>(schema);
    }

    @Override
    public LogFormat getFormat() {
        return LogFormat.AVRO;
    }

    @Override
    public CanonicalLog parse(byte[] input) throws FormatParseException {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(input);
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(bais, null);
            GenericRecord record = reader.read(null, decoder);

            @SuppressWarnings("unchecked")
            Map<CharSequence, CharSequence> avroMetadata = 
                (Map<CharSequence, CharSequence>) record.get("metadata");
            Map<String, String> metadata = new HashMap<>();
            if (avroMetadata != null) {
                avroMetadata.forEach((k, v) -> metadata.put(k.toString(), v.toString()));
            }

            return CanonicalLog.builder()
                    .id(record.get("id").toString())
                    .timestamp(Instant.ofEpochMilli((Long) record.get("timestamp")))
                    .level(CanonicalLog.LogLevel.fromString(record.get("level").toString()))
                    .service(record.get("service").toString())
                    .host(record.get("host").toString())
                    .message(record.get("message").toString())
                    .traceId(record.get("traceId") != null ? record.get("traceId").toString() : null)
                    .spanId(record.get("spanId") != null ? record.get("spanId").toString() : null)
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            throw new FormatParseException("Failed to parse Avro: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] serialize(CanonicalLog log) throws FormatSerializeException {
        try {
            GenericRecord record = new GenericData.Record(schema);
            record.put("id", log.getId());
            record.put("timestamp", log.getTimestamp().toEpochMilli());
            record.put("level", log.getLevel().name());
            record.put("service", log.getService());
            record.put("host", log.getHost());
            record.put("message", log.getMessage());
            record.put("traceId", log.getTraceId());
            record.put("spanId", log.getSpanId());

            Map<String, String> metadata = log.getMetadata();
            record.put("metadata", metadata != null ? metadata : new HashMap<>());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(baos, null);
            writer.write(record, encoder);
            encoder.flush();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new FormatSerializeException("Failed to serialize to Avro: " + e.getMessage(), e);
        }
    }
}
