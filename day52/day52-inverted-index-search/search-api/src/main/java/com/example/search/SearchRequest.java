package com.example.search;

import lombok.Data;

@Data
public class SearchRequest {
    private String query;
    private Integer limit = 100;
    private Boolean withScores = false;
}
