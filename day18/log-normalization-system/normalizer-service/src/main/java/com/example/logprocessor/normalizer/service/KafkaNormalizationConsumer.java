package com.example.logprocessor.normalizer.service;

import com.example.logprocessor.common.format.LogFormat;
import com.example.logprocessor.common.model.NormalizationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaNormalizationConsumer {

    private final NormalizationService normalizationService;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @KafkaListener(topics = "${app.kafka.input-topic:raw-logs}", 
                   groupId = "${app.kafka.group-id:normalizer-group}")
    public void consumeAndNormalize(ConsumerRecord<String, byte[]> record) {
        log.debug("Received log from partition {} offset {}", 
                record.partition(), record.offset());

        try {
            // Get target format from header or use default
            LogFormat targetFormat = getTargetFormat(record);
            String contentType = getContentType(record);

            NormalizationResult result = normalizationService.normalize(
                    record.value(), targetFormat, contentType);

            if (result.isSuccess()) {
                // Send to output topic
                String outputTopic = getOutputTopic(targetFormat);
                kafkaTemplate.send(outputTopic, record.key(), result.getData());
                log.debug("Normalized {} -> {} sent to {}", 
                        result.getSourceFormat(), targetFormat, outputTopic);
            } else {
                // Send to dead letter queue
                kafkaTemplate.send("normalization-dlq", record.key(), record.value());
                log.warn("Normalization failed: {}", result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Error processing record: {}", e.getMessage(), e);
            kafkaTemplate.send("normalization-dlq", record.key(), record.value());
        }
    }

    private LogFormat getTargetFormat(ConsumerRecord<String, byte[]> record) {
        var header = record.headers().lastHeader("target-format");
        if (header != null) {
            return LogFormat.valueOf(new String(header.value()));
        }
        return LogFormat.AVRO; // Default target format
    }

    private String getContentType(ConsumerRecord<String, byte[]> record) {
        var header = record.headers().lastHeader("content-type");
        return header != null ? new String(header.value()) : null;
    }

    private String getOutputTopic(LogFormat format) {
        return switch (format) {
            case JSON -> "normalized-logs-json";
            case AVRO -> "normalized-logs-avro";
            case PROTOBUF -> "normalized-logs-protobuf";
            case TEXT -> "normalized-logs-text";
        };
    }
}
