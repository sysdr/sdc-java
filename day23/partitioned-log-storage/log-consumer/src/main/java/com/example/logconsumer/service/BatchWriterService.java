package com.example.logconsumer.service;

import com.example.logconsumer.model.LogEntry;
import com.example.logconsumer.repository.LogRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Batches database writes to amortize partition routing cost.
 * Single writes pay 2ms routing penalty; batching 1000 writes reduces per-write cost to 0.002ms.
 */
@Service
public class BatchWriterService {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchWriterService.class);
    private static final int BATCH_SIZE = 1000;
    
    @Autowired
    private LogRepository logRepository;
    
    private final BlockingQueue<LogEntry> writeQueue;
    private final Counter writtenCounter;
    private final Timer batchTimer;
    
    public BatchWriterService(MeterRegistry meterRegistry) {
        this.writeQueue = new LinkedBlockingQueue<>(10000);
        this.writtenCounter = Counter.builder("logs.written.total")
                .description("Total logs written to database")
                .register(meterRegistry);
        this.batchTimer = Timer.builder("logs.batch.duration")
                .description("Batch write duration")
                .register(meterRegistry);
    }
    
    public void enqueue(LogEntry logEntry) {
        if (!writeQueue.offer(logEntry)) {
            logger.warn("Write queue full, dropping log");
        }
    }
    
    /**
     * Flush batches every second or when batch size reached.
     * Scheduled execution prevents queue buildup during low traffic.
     */
    @Scheduled(fixedRate = 1000)
    @Transactional
    public void flushBatch() {
        List<LogEntry> batch = new ArrayList<>(BATCH_SIZE);
        writeQueue.drainTo(batch, BATCH_SIZE);
        
        if (batch.isEmpty()) {
            return;
        }
        
        batchTimer.record(() -> {
            try {
                logRepository.saveAll(batch);
                writtenCounter.increment(batch.size());
                logger.debug("Flushed batch of {} logs", batch.size());
            } catch (Exception e) {
                logger.error("Failed to write batch", e);
                // Re-queue failed items for retry
                writeQueue.addAll(batch);
            }
        });
    }
}
