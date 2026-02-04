package com.example.logprocessor.producer.config;

import com.example.logprocessor.producer.model.DeadLetterEvent;
import com.example.logprocessor.producer.service.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Background task: periodically attempt to re-produce dead-lettered events.
 * Only runs every 30 seconds — we don't want to hammer a recovering broker.
 *
 * Note: produce() is still wrapped with the circuit breaker, so if the broker
 * is still down, events will just go back to the buffer via the fallback.
 */
@Component
public class DlqReconciler {
    private static final Logger LOG = LoggerFactory.getLogger(DlqReconciler.class);

    private final KafkaProducerService kafkaProducerService;

    public DlqReconciler(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @Scheduled(fixedDelay = 30_000)  // 30-second delay AFTER last run completes
    public void reconcile() {
        int bufferSize = kafkaProducerService.getDeadLetterBufferSize();
        if (bufferSize == 0) return;

        LOG.info("[DLQ-RECONCILER] Attempting to re-produce {} buffered events.", bufferSize);

        List<DeadLetterEvent> events = kafkaProducerService.drainDeadLetterBuffer();
        int reProduced = 0;
        for (DeadLetterEvent dle : events) {
            try {
                kafkaProducerService.produce(dle.originalEvent());
                reProduced++;
            } catch (Exception ex) {
                LOG.warn("[DLQ-RECONCILER] Re-produce failed for {}: {}", dle.originalEvent().eventId(), ex.getMessage());
                // Event will re-enter the buffer via the fallback in produce()
            }
        }
        LOG.info("[DLQ-RECONCILER] Re-produced {}/{} events.", reProduced, events.size());
    }
}
