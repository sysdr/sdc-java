package com.example.logindexing.search;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchService {

    private final LogDocumentRepository documentRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final Timer searchLatency;
    private final Counter searchQueries;
    private final Counter cacheHits;
    private final Counter cacheMisses;

    public SearchService(LogDocumentRepository documentRepository,
                        RedisTemplate<String, String> redisTemplate,
                        MeterRegistry meterRegistry) {
        this.documentRepository = documentRepository;
        this.redisTemplate = redisTemplate;
        this.searchLatency = Timer.builder("search.latency")
                .description("Time to execute search query")
                .register(meterRegistry);
        this.searchQueries = Counter.builder("search.queries.total")
                .description("Total search queries")
                .register(meterRegistry);
        this.cacheHits = Counter.builder("search.cache.hits")
                .description("Search cache hits")
                .register(meterRegistry);
        this.cacheMisses = Counter.builder("search.cache.misses")
                .description("Search cache misses")
                .register(meterRegistry);
    }

    @CircuitBreaker(name = "search", fallbackMethod = "searchFallback")
    public SearchResponse search(SearchRequest request) {
        searchQueries.increment();
        long startTime = System.currentTimeMillis();
        
        try {
            // Try cache first
            String cacheKey = buildCacheKey(request);
            List<String> cachedDocIds = getCachedResults(cacheKey);
            
            List<LogDocument> results;
            if (cachedDocIds != null && !cachedDocIds.isEmpty()) {
                cacheHits.increment();
                results = fetchDocuments(cachedDocIds);
            } else {
                cacheMisses.increment();
                // Simulate index search (in real implementation, would query InvertedIndex)
                results = performDatabaseSearch(request);
                cacheResults(cacheKey, results);
            }
            
            long queryTime = System.currentTimeMillis() - startTime;
            searchLatency.record(queryTime, java.util.concurrent.TimeUnit.MILLISECONDS);
            
            return SearchResponse.builder()
                    .results(results)
                    .totalResults(results.size())
                    .queryTimeMs(queryTime)
                    .query(request.getQuery())
                    .build();
                    
        } catch (Exception e) {
            log.error("Search failed", e);
            throw e;
        }
    }

    private List<LogDocument> performDatabaseSearch(SearchRequest request) {
        // Simplified search using JPA
        // In production, this would query the InvertedIndex and then fetch documents
        List<LogDocument> allDocs = documentRepository.findAll();
        
        return allDocs.stream()
                .filter(doc -> matchesFilter(doc, request))
                .limit(request.getLimit())
                .collect(Collectors.toList());
    }

    private boolean matchesFilter(LogDocument doc, SearchRequest request) {
        if (request.getLevel() != null && !request.getLevel().equals(doc.getLevel())) {
            return false;
        }
        if (request.getService() != null && !request.getService().equals(doc.getService())) {
            return false;
        }
        if (request.getUserId() != null && !request.getUserId().equals(doc.getUserId())) {
            return false;
        }
        if (request.getQuery() != null && !doc.getMessage().toLowerCase()
                .contains(request.getQuery().toLowerCase())) {
            return false;
        }
        return true;
    }

    private String buildCacheKey(SearchRequest request) {
        return String.format("search:%s:%s:%s", 
            request.getQuery(), request.getLevel(), request.getService());
    }

    private List<String> getCachedResults(String cacheKey) {
        try {
            Set<String> members = redisTemplate.opsForSet().members(cacheKey);
            return members != null ? new ArrayList<>(members) : null;
        } catch (Exception e) {
            log.warn("Cache read failed", e);
            return null;
        }
    }

    private void cacheResults(String cacheKey, List<LogDocument> results) {
        try {
            String[] docIds = results.stream()
                    .map(LogDocument::getId)
                    .toArray(String[]::new);
            if (docIds.length > 0) {
                redisTemplate.opsForSet().add(cacheKey, docIds);
                redisTemplate.expire(cacheKey, java.time.Duration.ofMinutes(5));
            }
        } catch (Exception e) {
            log.warn("Cache write failed", e);
        }
    }

    private List<LogDocument> fetchDocuments(List<String> docIds) {
        return documentRepository.findAllById(docIds);
    }

    private SearchResponse searchFallback(SearchRequest request, Exception e) {
        log.error("Search circuit breaker activated", e);
        return SearchResponse.builder()
                .results(Collections.emptyList())
                .totalResults(0)
                .queryTimeMs(0)
                .query(request.getQuery())
                .build();
    }
}
