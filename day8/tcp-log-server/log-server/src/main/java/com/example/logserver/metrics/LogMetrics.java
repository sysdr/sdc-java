package com.example.logserver.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Metrics for TCP log server monitoring.
 * Exposed via /actuator/prometheus endpoint.
 */
@Component
@Slf4j
public class LogMetrics {

    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final Counter receivedMessages;
    private final Counter droppedMessages;
    private final Counter invalidMessages;
    private final Counter processingErrors;
    private final Counter connectionErrors;
    private final Counter idleTimeouts;
    private final Counter batchesWritten;
    private final Counter writeFailures;
    private final Counter bufferOverflows;
    private final Timer batchWriteTime;

    public LogMetrics(MeterRegistry registry) {
        // Gauge for active connections
        registry.gauge("tcp_active_connections", activeConnections);

        // Counters
        this.receivedMessages = Counter.builder("log_messages_received_total")
            .description("Total log messages received")
            .register(registry);

        this.droppedMessages = Counter.builder("log_messages_dropped_total")
            .description("Total log messages dropped due to buffer full")
            .register(registry);

        this.invalidMessages = Counter.builder("log_messages_invalid_total")
            .description("Total invalid log messages")
            .register(registry);

        this.processingErrors = Counter.builder("log_processing_errors_total")
            .description("Total processing errors")
            .register(registry);

        this.connectionErrors = Counter.builder("tcp_connection_errors_total")
            .description("Total TCP connection errors")
            .register(registry);

        this.idleTimeouts = Counter.builder("tcp_idle_timeouts_total")
            .description("Total idle timeout disconnections")
            .register(registry);

        this.batchesWritten = Counter.builder("db_batches_written_total")
            .description("Total database batches written")
            .register(registry);

        this.writeFailures = Counter.builder("db_write_failures_total")
            .description("Total database write failures")
            .register(registry);

        this.bufferOverflows = Counter.builder("buffer_overflows_total")
            .description("Total buffer overflow events")
            .register(registry);

        // Timer
        this.batchWriteTime = Timer.builder("db_batch_write_duration")
            .description("Time to write batch to database")
            .register(registry);
    }

    public void incrementActiveConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementActiveConnections() {
        activeConnections.decrementAndGet();
    }

    public void incrementReceivedMessages() {
        receivedMessages.increment();
    }

    public void incrementDroppedMessages() {
        droppedMessages.increment();
    }

    public void incrementInvalidMessages() {
        invalidMessages.increment();
    }

    public void incrementProcessingErrors() {
        processingErrors.increment();
    }

    public void incrementConnectionErrors() {
        connectionErrors.increment();
    }

    public void incrementIdleTimeouts() {
        idleTimeouts.increment();
    }

    public void recordBatchWritten(int size) {
        batchesWritten.increment();
    }

    public void recordWriteFailure() {
        writeFailures.increment();
    }

    public void recordBufferOverflow() {
        bufferOverflows.increment();
    }

    public void recordBufferSize(int size) {
        // Can add gauge if needed
    }

    public void recordServerStarted() {
        log.info("Log server metrics initialized");
    }
}
