package com.example.facetedsearch;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class FacetedSearchResponse {
    private List<LogDocument> results;
    private long totalHits;
    private Map<String, Map<String, Long>> facets;  // field -> (value -> count)
    private long queryTimeMs;
    private boolean fromCache;
}
