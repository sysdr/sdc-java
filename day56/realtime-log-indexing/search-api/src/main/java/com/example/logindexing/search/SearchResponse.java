package com.example.logindexing.search;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResponse {
    private List<LogDocument> results;
    private int totalResults;
    private long queryTimeMs;
    private String query;
}
