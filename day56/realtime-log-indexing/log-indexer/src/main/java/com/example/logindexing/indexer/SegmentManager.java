package com.example.logindexing.indexer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SegmentManager {

    private final InvertedIndex invertedIndex;
    private static final int REFRESH_THRESHOLD = 1000; // documents

    public SegmentManager(InvertedIndex invertedIndex) {
        this.invertedIndex = invertedIndex;
    }

    @Scheduled(fixedRate = 1000) // Every 1 second (refresh interval)
    public void refreshSegment() {
        int docCount = invertedIndex.getDocumentCount();
        
        if (docCount >= REFRESH_THRESHOLD) {
            log.info("Refreshing segment: {} documents, {} terms", 
                docCount, invertedIndex.getTermCount());
            
            // Increment segment version (makes data searchable)
            invertedIndex.incrementSegmentVersion();
            
            log.info("Segment refreshed to version {}", 
                invertedIndex.getSegmentVersion());
        }
    }

    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void reportSegmentMetrics() {
        log.info("Segment metrics - Version: {}, Documents: {}, Terms: {}",
            invertedIndex.getSegmentVersion(),
            invertedIndex.getDocumentCount(),
            invertedIndex.getTermCount());
    }
}
