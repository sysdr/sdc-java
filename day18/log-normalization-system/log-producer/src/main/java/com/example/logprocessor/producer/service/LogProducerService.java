package com.example.logprocessor.producer.service;

import com.example.logprocessor.common.format.LogFormat;
import com.example.logprocessor.producer.generator.MultiFormatLogGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogProducerService {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final MultiFormatLogGenerator generator;

    @Value("${app.kafka.topic:raw-logs}")
    private String topic;

    @Value("${app.producer.enabled:false}")
    private boolean producerEnabled;

    private final AtomicLong messageCount = new AtomicLong(0);

    @Scheduled(fixedRateString = "${app.producer.rate-ms:100}")
    public void produceRandomLog() {
        if (!producerEnabled) return;

        LogFormat format = randomFormat();
        byte[] data = generator.generate(format);
        
        sendLog(data, format);
    }

    public void sendLog(byte[] data, LogFormat format) {
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, data);
        record.headers().add("content-type", format.getContentType().getBytes());
        record.headers().add("source-format", format.name().getBytes());

        kafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send log: {}", ex.getMessage());
                    } else {
                        long count = messageCount.incrementAndGet();
                        if (count % 1000 == 0) {
                            log.info("Sent {} messages", count);
                        }
                    }
                });
    }

    public void sendBatch(int count, LogFormat format) {
        for (int i = 0; i < count; i++) {
            byte[] data = generator.generate(format);
            sendLog(data, format);
        }
        log.info("Sent batch of {} {} messages", count, format);
    }

    private LogFormat randomFormat() {
        LogFormat[] formats = LogFormat.values();
        return formats[ThreadLocalRandom.current().nextInt(formats.length)];
    }

    public long getMessageCount() {
        return messageCount.get();
    }
}
