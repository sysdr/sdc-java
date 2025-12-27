package com.example.logconsumer.listener;

import com.example.logconsumer.model.LogEvent;
import com.example.logconsumer.service.LogProcessingService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LogEventListener {

    private final LogProcessingService processingService;
    private final Counter acknowledgedCounter;
    private final Counter rejectedCounter;
    private final Timer processingTimer;

    public LogEventListener(LogProcessingService processingService,
                           MeterRegistry meterRegistry) {
        this.processingService = processingService;
        this.acknowledgedCounter = meterRegistry.counter("consumer.messages.acknowledged");
        this.rejectedCounter = meterRegistry.counter("consumer.messages.rejected");
        this.processingTimer = meterRegistry.timer("consumer.processing.time");
    }

    @KafkaListener(topics = "log-events", groupId = "log-consumer-group")
    public void listen(@Payload LogEvent event,
                      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                      @Header(KafkaHeaders.OFFSET) long offset,
                      Acknowledgment acknowledgment) {
        
        log.info("Received message: id={}, partition={}, offset={}", 
                 event.getId(), partition, offset);

        Timer.Sample sample = Timer.start();
        
        try {
            processingService.processLog(event);
            acknowledgment.acknowledge();
            acknowledgedCounter.increment();
            log.info("Message acknowledged: id={}", event.getId());
        } catch (Exception e) {
            log.error("Processing failed permanently for message: id={}", event.getId(), e);
            acknowledgment.acknowledge();
            rejectedCounter.increment();
        } finally {
            sample.stop(processingTimer);
        }
    }
}
