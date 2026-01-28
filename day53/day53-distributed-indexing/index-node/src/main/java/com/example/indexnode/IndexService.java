package com.example.indexnode;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class IndexService {
    private static final Logger log = LoggerFactory.getLogger(IndexService.class);

    private final StandardAnalyzer analyzer = new StandardAnalyzer();
    private Directory directory;
    private IndexWriter writer;
    private SearcherManager searcherManager;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicInteger pendingDocs = new AtomicInteger(0);

    private final Counter indexedDocsCounter;
    private final Counter searchQueriesCounter;
    private final Timer indexTimer;
    private final Timer searchTimer;

    @Value("${index.node.id}")
    private String nodeId;

    @Value("${index.flush.interval:30}")
    private int flushIntervalSeconds;

    @Value("${index.flush.threshold:10000}")
    private int flushThreshold;

    public IndexService(MeterRegistry meterRegistry) {
        this.indexedDocsCounter = Counter.builder("index.documents.indexed")
                .description("Total documents indexed")
                .register(meterRegistry);
        this.searchQueriesCounter = Counter.builder("index.queries.executed")
                .description("Total search queries executed")
                .register(meterRegistry);
        this.indexTimer = Timer.builder("index.operation.duration")
                .description("Time to index a document")
                .register(meterRegistry);
        this.searchTimer = Timer.builder("search.operation.duration")
                .description("Time to execute a search")
                .register(meterRegistry);
    }

    @PostConstruct
    public void initialize() throws IOException {
        log.info("Initializing index node: {}", nodeId);
        
        // In-memory index for demo; use FSDirectory for production persistence
        directory = new ByteBuffersDirectory();
        
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        config.setRAMBufferSizeMB(256.0);
        
        writer = new IndexWriter(directory, config);
        searcherManager = new SearcherManager(writer, null);

        // Schedule periodic flush
        scheduler.scheduleAtFixedRate(this::periodicFlush, 
                flushIntervalSeconds, flushIntervalSeconds, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        try {
            scheduler.shutdown();
            if (writer != null) {
                writer.commit();
                writer.close();
            }
            if (directory != null) {
                directory.close();
            }
        } catch (IOException e) {
            log.error("Error shutting down index", e);
        }
    }

    public void indexLog(LogEntry logEntry) {
        indexTimer.record(() -> {
            try {
                Document doc = new Document();
                
                // Stored fields
                doc.add(new StringField("logId", logEntry.getLogId(), Field.Store.YES));
                doc.add(new StringField("tenantId", logEntry.getTenantId(), Field.Store.YES));
                doc.add(new LongPoint("timestamp", logEntry.getTimestamp()));
                doc.add(new StoredField("timestamp", logEntry.getTimestamp()));
                doc.add(new StringField("level", logEntry.getLevel(), Field.Store.YES));
                doc.add(new StringField("service", logEntry.getService(), Field.Store.YES));
                
                // Full-text searchable field
                doc.add(new TextField("message", logEntry.getMessage(), Field.Store.YES));
                
                writer.addDocument(doc);
                indexedDocsCounter.increment();
                
                int pending = pendingDocs.incrementAndGet();
                if (pending >= flushThreshold) {
                    flush();
                }
                
                log.debug("Indexed log: {} on node {}", logEntry.getLogId(), nodeId);
            } catch (IOException e) {
                log.error("Failed to index log: {}", logEntry.getLogId(), e);
                throw new RuntimeException("Indexing failed", e);
            }
        });
    }

    public SearchResult search(String queryString, int limit) {
        return searchTimer.record(() -> {
            try {
                searchQueriesCounter.increment();
                long startTime = System.currentTimeMillis();
                
                QueryParser parser = new QueryParser("message", analyzer);
                Query query = parser.parse(queryString);
                
                searcherManager.maybeRefresh();
                IndexSearcher searcher = searcherManager.acquire();
                
                try {
                    TopDocs topDocs = searcher.search(query, limit);
                    
                    List<LogEntry> results = new ArrayList<>();
                    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                        Document doc = searcher.doc(scoreDoc.doc);
                        LogEntry entry = new LogEntry();
                        entry.setLogId(doc.get("logId"));
                        entry.setTenantId(doc.get("tenantId"));
                        entry.setTimestamp(doc.getField("timestamp").numericValue().longValue());
                        entry.setLevel(doc.get("level"));
                        entry.setMessage(doc.get("message"));
                        entry.setService(doc.get("service"));
                        results.add(entry);
                    }
                    
                    long searchTime = System.currentTimeMillis() - startTime;
                    log.info("Search '{}' returned {} results in {}ms on node {}", 
                            queryString, topDocs.totalHits.value, searchTime, nodeId);
                    
                    return new SearchResult(results, (int) topDocs.totalHits.value, searchTime);
                } finally {
                    searcherManager.release(searcher);
                }
            } catch (Exception e) {
                log.error("Search failed for query: {}", queryString, e);
                throw new RuntimeException("Search failed", e);
            }
        });
    }

    private void periodicFlush() {
        try {
            if (pendingDocs.get() > 0) {
                flush();
                log.info("Periodic flush completed on node {}", nodeId);
            }
        } catch (Exception e) {
            log.error("Periodic flush failed", e);
        }
    }

    private void flush() {
        try {
            writer.commit();
            searcherManager.maybeRefresh();
            pendingDocs.set(0);
        } catch (IOException e) {
            log.error("Flush failed", e);
            throw new RuntimeException("Flush failed", e);
        }
    }

    public IndexStats getStats() {
        return new IndexStats(
                nodeId,
                writer.getDocStats().numDocs,
                writer.getDocStats().maxDoc,
                pendingDocs.get()
        );
    }

    @Data
    @AllArgsConstructor
    public static class IndexStats {
        private String nodeId;
        private long numDocs;
        private long maxDoc;
        private int pendingDocs;
    }
}
