package com.example.logindexing.search;

import lombok.Data;

@Data
public class SearchRequest {
    private String query;
    private String level;
    private String service;
    private String userId;
    private Long fromTimestamp;
    private Long toTimestamp;
    private int limit = 100;
    private int offset = 0;
}
