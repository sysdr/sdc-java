package com.example.logprocessor.consumer.config;

import com.example.logprocessor.consumer.model.LogEventEntity;
import com.example.logprocessor.consumer.service.PostgresWriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically flushes the in-memory write buffer back to PostgreSQL.
 * Only effective when the postgresWriter breaker has closed.
 */
@Component
public class WriteBufferReconciler {
    private static final Logger LOG = LoggerFactory.getLogger(WriteBufferReconciler.class);

    private final PostgresWriteService postgresWriteService;

    public WriteBufferReconciler(PostgresWriteService postgresWriteService) {
        this.postgresWriteService = postgresWriteService;
    }

    @Scheduled(fixedDelay = 20_000)  // 20-second delay after last run
    public void reconcile() {
        int bufferSize = postgresWriteService.getWriteBufferSize();
        if (bufferSize == 0) return;

        LOG.info("[WRITE-RECONCILER] Flushing {} buffered events to PostgreSQL.", bufferSize);

        List<LogEventEntity> events = postgresWriteService.drainWriteBuffer();
        int persisted = 0;
        for (LogEventEntity entity : events) {
            try {
                postgresWriteService.persist(entity);
                persisted++;
            } catch (Exception ex) {
                LOG.warn("[WRITE-RECONCILER] Re-persist failed for {}: {}", entity.getEventId(), ex.getMessage());
            }
        }
        LOG.info("[WRITE-RECONCILER] Persisted {}/{} buffered events.", persisted, events.size());
    }
}
