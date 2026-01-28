package com.example.search;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SearchService {
    
    private final IndexingServiceClient indexingClient;
    private final LogDocumentRepository documentRepository;
    private final Timer searchTimer;
    
    public SearchService(IndexingServiceClient indexingClient,
                        LogDocumentRepository documentRepository,
                        MeterRegistry meterRegistry) {
        this.indexingClient = indexingClient;
        this.documentRepository = documentRepository;
        this.searchTimer = Timer.builder("search_query_duration")
            .description("Time taken to execute search queries")
            .register(meterRegistry);
    }
    
    public SearchResult search(String query, Integer limit, Boolean withScores) {
        return searchTimer.record(() -> {
            long startTime = System.currentTimeMillis();
            List<Long> docIds = indexingClient.search(query, withScores);
            if (docIds.isEmpty()) {
                return new SearchResult(query, 0, 
                    System.currentTimeMillis() - startTime, List.of());
            }
            List<LogDocument> documents = documentRepository.findByIdIn(
                docIds.subList(0, Math.min(limit, docIds.size()))
            );
            long searchTime = System.currentTimeMillis() - startTime;
            log.info("Search completed: query='{}', results={}, time={}ms", 
                query, documents.size(), searchTime);
            return new SearchResult(query, documents.size(), searchTime, documents);
        });
    }
}
