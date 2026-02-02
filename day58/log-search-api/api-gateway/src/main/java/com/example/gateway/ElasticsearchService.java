package com.example.gateway;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;

    public Mono<com.example.gateway.SearchResponse> search(Query query, String cursor, int limit) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                    .index("logs-*")
                    .query(query)
                    .size(limit)
                    .sort(s -> s.field(f -> f.field("timestamp").order(SortOrder.Desc)))
                    .sort(s -> s.field(f -> f.field("_id").order(SortOrder.Asc)));

            // Add search_after for cursor-based pagination
            if (cursor != null) {
                Object[] sortValues = decodeCursor(cursor);
                List<FieldValue> searchAfter = new ArrayList<>();
                for (Object value : sortValues) {
                    if (value instanceof String) {
                        searchAfter.add(FieldValue.of((String) value));
                    } else if (value instanceof Number) {
                        searchAfter.add(FieldValue.of(((Number) value).longValue()));
                    } else {
                        searchAfter.add(FieldValue.of(value.toString()));
                    }
                }
                requestBuilder.searchAfter(searchAfter);
            }

            SearchResponse<LogDocument> response = elasticsearchClient.search(
                    requestBuilder.build(),
                    LogDocument.class);

            List<com.example.gateway.SearchResponse.LogEntry> logs = new ArrayList<>();
            String nextCursor = null;

            if (response.hits().hits() != null && !response.hits().hits().isEmpty()) {
                for (Hit<LogDocument> hit : response.hits().hits()) {
                    LogDocument doc = hit.source();
                    if (doc != null) {
                        logs.add(new com.example.gateway.SearchResponse.LogEntry(
                                doc.getTimestamp(),
                                doc.getService(),
                                doc.getLevel(),
                                doc.getMessage(),
                                doc.getTraceId()));
                    }
                }

                // Generate cursor for next page
                Hit<LogDocument> lastHit = response.hits().hits().get(response.hits().hits().size() - 1);
                if (lastHit.sort() != null) {
                    nextCursor = encodeCursor(lastHit.sort().toArray());
                }
            }

            long queryTime = System.currentTimeMillis() - startTime;
            long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;

            log.info("Search completed: {} hits in {}ms", totalHits, queryTime);

            return new com.example.gateway.SearchResponse(
                    logs,
                    nextCursor,
                    totalHits,
                    queryTime,
                    false);
        });
    }

    private String encodeCursor(Object[] sortValues) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(sortValues);
            return Base64.getUrlEncoder().encodeToString(json);
        } catch (Exception e) {
            log.error("Failed to encode cursor", e);
            return null;
        }
    }

    private Object[] decodeCursor(String cursor) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(cursor);
            return objectMapper.readValue(json, Object[].class);
        } catch (Exception e) {
            throw new InvalidQueryException("Invalid cursor: " + cursor);
        }
    }
}
