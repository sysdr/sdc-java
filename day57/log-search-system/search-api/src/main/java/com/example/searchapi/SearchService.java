package com.example.searchapi;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private static final String INDEX_NAME = "logs";
    
    private final ElasticsearchClient esClient;
    private final Counter searchRequests;
    private final Counter searchErrors;
    private final Timer searchTimer;
    
    public SearchService(ElasticsearchClient esClient, MeterRegistry meterRegistry) {
        this.esClient = esClient;
        this.searchRequests = Counter.builder("search.requests")
                .description("Total search requests")
                .register(meterRegistry);
        this.searchErrors = Counter.builder("search.errors")
                .description("Search errors")
                .register(meterRegistry);
        this.searchTimer = Timer.builder("search.duration")
                .description("Search query duration")
                .register(meterRegistry);
    }
    
    @CircuitBreaker(name = "elasticsearch", fallbackMethod = "searchFallback")
    public SearchResponse search(com.example.searchapi.SearchRequest request) {
        searchRequests.increment();
        
        return searchTimer.record(() -> {
            try {
                Query mainQuery = buildMultiMatchQuery(request.getQuery());
                Query finalQuery = applyFilters(mainQuery, request);
                
                SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(finalQuery)
                    .from(request.getPage() * request.getSize())
                    .size(request.getSize())
                );
                
                var response = esClient.search(searchRequest, Object.class);
                
                List<SearchResponse.SearchHit> hits = new ArrayList<>();
                for (Hit<Object> hit : response.hits().hits()) {
                    hits.add(SearchResponse.SearchHit.builder()
                        .id(hit.id())
                        .score(hit.score() != null ? hit.score() : 0.0)
                        .source((java.util.Map<String, Object>) hit.source())
                        .build());
                }
                
                return SearchResponse.builder()
                    .totalHits(response.hits().total().value())
                    .hits(hits)
                    .tookMs(response.took())
                    .build();
                    
            } catch (IOException e) {
                searchErrors.increment();
                logger.error("Search failed", e);
                throw new RuntimeException("Search failed", e);
            }
        });
    }
    
    private Query buildMultiMatchQuery(String queryText) {
        if (queryText == null || queryText.trim().isEmpty()) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        
        // Multi-match with field boosting
        MultiMatchQuery multiMatch = MultiMatchQuery.of(m -> m
            .query(queryText)
            .fields("message^3", "service_name^2", "stack_trace^1")
            .type(TextQueryType.BestFields)
            .fuzziness("AUTO")
        );
        
        return Query.of(q -> q.multiMatch(multiMatch));
    }
    
    private Query applyFilters(Query mainQuery, com.example.searchapi.SearchRequest request) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder().must(mainQuery);
        
        if (request.getSeverities() != null && !request.getSeverities().isEmpty()) {
            boolBuilder.filter(f -> f.terms(t -> t
                .field("severity")
                .terms(ts -> ts.value(request.getSeverities().stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList())))
            ));
        }
        
        if (request.getServiceNames() != null && !request.getServiceNames().isEmpty()) {
            boolBuilder.filter(f -> f.terms(t -> t
                .field("service_name")
                .terms(ts -> ts.value(request.getServiceNames().stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList())))
            ));
        }
        
        if (request.getStartTime() != null || request.getEndTime() != null) {
            boolBuilder.filter(f -> f.range(r -> {
                r.field("timestamp");
                if (request.getStartTime() != null) {
                    r.gte(JsonData.of(request.getStartTime()));
                }
                if (request.getEndTime() != null) {
                    r.lte(JsonData.of(request.getEndTime()));
                }
                return r;
            }));
        }
        
        return Query.of(q -> q.bool(boolBuilder.build()));
    }
    
    public SearchResponse searchFallback(com.example.searchapi.SearchRequest request, Exception e) {
        logger.error("Circuit breaker activated, returning fallback", e);
        return SearchResponse.builder()
            .totalHits(0)
            .hits(List.of())
            .tookMs(0)
            .build();
    }
}
