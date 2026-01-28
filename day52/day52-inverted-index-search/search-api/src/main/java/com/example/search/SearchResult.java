package com.example.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private String query;
    private Integer totalResults;
    private Long searchTimeMs;
    private List<LogDocument> documents;
}
