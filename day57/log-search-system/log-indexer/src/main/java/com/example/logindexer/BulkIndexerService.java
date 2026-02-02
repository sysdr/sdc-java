package com.example.logindexer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class BulkIndexerService {
    private static final Logger logger = LoggerFactory.getLogger(BulkIndexerService.class);
    private static final String INDEX_NAME = "logs";
    private static final int BATCH_SIZE = 1000;
    
    private final ElasticsearchClient esClient;
    private final List<LogEvent> buffer = new ArrayList<>(BATCH_SIZE);
    private final Lock bufferLock = new ReentrantLock();
    
    private final Counter documentsIndexed;
    private final Counter indexingErrors;
    private final Timer bulkIndexTimer;
    
    public BulkIndexerService(ElasticsearchClient esClient, MeterRegistry meterRegistry) {
        this.esClient = esClient;
        this.documentsIndexed = Counter.builder("elasticsearch.documents.indexed")
                .description("Total documents indexed to Elasticsearch")
                .register(meterRegistry);
        this.indexingErrors = Counter.builder("elasticsearch.indexing.errors")
                .description("Indexing errors")
                .register(meterRegistry);
        this.bulkIndexTimer = Timer.builder("elasticsearch.bulk.duration")
                .description("Time taken for bulk indexing")
                .register(meterRegistry);
    }
    
    public void addToBuffer(LogEvent event) {
        bufferLock.lock();
        try {
            buffer.add(event);
            if (buffer.size() >= BATCH_SIZE) {
                flushBuffer();
            }
        } finally {
            bufferLock.unlock();
        }
    }
    
    @Scheduled(fixedDelay = 1000)
    public void scheduledFlush() {
        bufferLock.lock();
        try {
            if (!buffer.isEmpty()) {
                flushBuffer();
            }
        } finally {
            bufferLock.unlock();
        }
    }
    
    private void flushBuffer() {
        if (buffer.isEmpty()) return;
        
        List<LogEvent> batch = new ArrayList<>(buffer);
        buffer.clear();
        
        bulkIndexTimer.record(() -> {
            try {
                BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
                
                for (LogEvent event : batch) {
                    bulkBuilder.operations(op -> op
                        .index(idx -> idx
                            .index(INDEX_NAME)
                            .id(event.getId())
                            .document(event)
                        )
                    );
                }
                
                BulkResponse response = esClient.bulk(bulkBuilder.build());
                
                if (response.errors()) {
                    for (BulkResponseItem item : response.items()) {
                        if (item.error() != null) {
                            indexingErrors.increment();
                            logger.error("Failed to index document {}: {}", 
                                item.id(), item.error().reason());
                        }
                    }
                } else {
                    documentsIndexed.increment(batch.size());
                    logger.info("Successfully indexed {} documents", batch.size());
                }
            } catch (IOException e) {
                indexingErrors.increment(batch.size());
                logger.error("Bulk indexing failed", e);
            }
        });
    }
}
