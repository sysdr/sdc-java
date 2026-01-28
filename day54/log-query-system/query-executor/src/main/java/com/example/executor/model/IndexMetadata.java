package com.example.executor.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class IndexMetadata {
    private String indexName;
    private String fieldName;
    private String indexType; // INVERTED, BTREE, HASH
    private long totalDocuments;
    private Map<String, Long> termFrequency;
    private double averageSelectivity;
}
