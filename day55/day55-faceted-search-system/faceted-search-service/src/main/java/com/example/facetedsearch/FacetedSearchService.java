package com.example.facetedsearch;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FacetedSearchService {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheService cacheService;

    private static final String[] FACET_FIELDS = {"level", "service", "environment", "host", "region", "errorType"};
    private static final int FACET_SIZE = 100;  // Top 100 values per facet

    @CircuitBreaker(name = "elasticsearchSearch", fallbackMethod = "searchFallback")
    public FacetedSearchResponse search(SearchRequest request) {
        long startTime = System.currentTimeMillis();

        // Try cache first
        String cacheKey = buildCacheKey(request);
        FacetedSearchResponse cached = cacheService.getCachedSearchResult(cacheKey);
        if (cached != null) {
            log.info("Cache hit for query: {}", cacheKey);
            cached.setQueryTimeMs(System.currentTimeMillis() - startTime);
            cached.setFromCache(true);
            return cached;
        }

        // Build Elasticsearch query
        BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();

        // Add filters for each dimension
        request.getFilters().forEach((field, value) -> {
            boolQueryBuilder.filter(f -> f.term(t -> t.field(field).value(value)));
        });

        // Add time range filter
        if (request.getFromTimestamp() != null || request.getToTimestamp() != null) {
            RangeQuery.Builder rangeBuilder = QueryBuilders.range().field("timestamp");
            if (request.getFromTimestamp() != null) {
                rangeBuilder.gte(JsonData.of(request.getFromTimestamp()));
            }
            if (request.getToTimestamp() != null) {
                rangeBuilder.lte(JsonData.of(request.getToTimestamp()));
            }
            boolQueryBuilder.filter(f -> f.range(rangeBuilder.build()));
        }

        // Add text search if provided
        if (request.getTextQuery() != null && !request.getTextQuery().isEmpty()) {
            boolQueryBuilder.must(m -> m.match(ma -> ma.field("message").query(request.getTextQuery())));
        }

        // Build query with aggregations
        var queryBuilder = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withPageable(PageRequest.of(request.getOffset() / request.getLimit(), request.getLimit()));

        // Add facet aggregations for each dimension
        for (String field : FACET_FIELDS) {
            queryBuilder.withAggregation(field, Aggregation.of(a -> a
                    .terms(t -> t.field(field).size(FACET_SIZE))
            ));
        }

        Query query = queryBuilder.build();

        // Execute search
        SearchHits<LogDocument> searchHits = elasticsearchTemplate.search(query, LogDocument.class);

        // Extract results
        List<LogDocument> results = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        // Extract facets from aggregations
        Map<String, Map<String, Long>> facets = new HashMap<>();
        if (searchHits.hasAggregations()) {
            var aggregations = searchHits.getAggregations();
            if (aggregations != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate> aggMap = 
                        (Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate>) aggregations.aggregations();
                    if (aggMap != null) {
                        for (String field : FACET_FIELDS) {
                            Map<String, Long> facetCounts = new HashMap<>();
                            try {
                                var aggContainer = aggMap.get(field);
                                if (aggContainer != null && aggContainer.isSterms()) {
                                    StringTermsAggregate agg = aggContainer.sterms();
                                    if (agg.buckets() != null && agg.buckets().array() != null) {
                                        for (StringTermsBucket bucket : agg.buckets().array()) {
                                            facetCounts.put(bucket.key().stringValue(), bucket.docCount());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("Failed to extract facet for field: {}", field, e);
                            }
                            facets.put(field, facetCounts);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to access aggregations", e);
                }
            }
        }

        long queryTimeMs = System.currentTimeMillis() - startTime;

        FacetedSearchResponse response = FacetedSearchResponse.builder()
                .results(results)
                .totalHits(searchHits.getTotalHits())
                .facets(facets)
                .queryTimeMs(queryTimeMs)
                .fromCache(false)
                .build();

        // Cache the result
        cacheService.cacheSearchResult(cacheKey, response);

        log.info("Search completed in {}ms, found {} results", queryTimeMs, searchHits.getTotalHits());
        return response;
    }

    public FacetedSearchResponse searchFallback(SearchRequest request, Exception e) {
        log.error("Elasticsearch search failed, returning fallback response", e);
        return FacetedSearchResponse.builder()
                .results(Collections.emptyList())
                .totalHits(0)
                .facets(Collections.emptyMap())
                .queryTimeMs(0)
                .fromCache(false)
                .build();
    }

    private String buildCacheKey(SearchRequest request) {
        // Build cache key from filters, time range, and text query
        StringBuilder keyBuilder = new StringBuilder("search:");
        
        request.getFilters().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> keyBuilder.append(e.getKey()).append("=").append(e.getValue()).append(":"));
        
        if (request.getTextQuery() != null) {
            keyBuilder.append("q=").append(request.getTextQuery()).append(":");
        }
        
        if (request.getFromTimestamp() != null) {
            keyBuilder.append("from=").append(request.getFromTimestamp()).append(":");
        }
        
        if (request.getToTimestamp() != null) {
            keyBuilder.append("to=").append(request.getToTimestamp());
        }
        
        return keyBuilder.toString();
    }
}
