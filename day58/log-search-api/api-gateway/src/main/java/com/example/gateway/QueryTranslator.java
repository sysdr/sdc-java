package com.example.gateway;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class QueryTranslator {

    private static final int MAX_WILDCARD_LENGTH = 3;
    private static final Set<String> ALLOWED_FIELDS = 
            Set.of("service", "level", "message", "timestamp", "traceId");

    public Query translate(SearchRequest request) {
        List<Query> mustClauses = new ArrayList<>();

        // Service filter (required)
        mustClauses.add(Query.of(q -> q
                .term(t -> t
                        .field("service.keyword")
                        .value(request.getService()))));

        // Level filter (optional)
        if (request.getLevel() != null) {
            mustClauses.add(Query.of(q -> q
                    .term(t -> t
                            .field("level")
                            .value(request.getLevel()))));
        }

        // Message search (optional, with wildcard validation)
        if (request.getMessage() != null) {
            validateWildcard(request.getMessage());
            
            if (request.getMessage().contains("*")) {
                mustClauses.add(Query.of(q -> q
                        .wildcard(w -> w
                                .field("message")
                                .value(request.getMessage()))));
            } else {
                mustClauses.add(Query.of(q -> q
                        .match(m -> m
                                .field("message")
                                .query(request.getMessage()))));
            }
        }

        // Time range filter
        String timeRangeValue = convertTimeRange(request.getTimeRange());
        mustClauses.add(Query.of(q -> q
                .range(r -> r
                        .field("timestamp")
                        .gte(JsonData.of(timeRangeValue)))));

        // Additional filters
        request.getFilters().forEach((field, value) -> {
            if (!ALLOWED_FIELDS.contains(field)) {
                throw new InvalidQueryException("Field not searchable: " + field);
            }
            mustClauses.add(Query.of(q -> q
                    .term(t -> t
                            .field(field + ".keyword")
                            .value(value))));
        });

        return Query.of(q -> q
                .bool(b -> b.must(mustClauses)));
    }

    private void validateWildcard(String value) {
        if (value.contains("*")) {
            String withoutWildcard = value.replace("*", "");
            if (withoutWildcard.length() < MAX_WILDCARD_LENGTH) {
                throw new InvalidQueryException(
                        "Wildcard query too broad. Minimum " + MAX_WILDCARD_LENGTH + 
                        " characters required: " + value);
            }
        }
    }

    private String convertTimeRange(String timeRange) {
        return switch (timeRange) {
            case "15m" -> "now-15m";
            case "1h" -> "now-1h";
            case "6h" -> "now-6h";
            case "24h" -> "now-24h";
            case "7d" -> "now-7d";
            default -> "now-1h";
        };
    }
}
