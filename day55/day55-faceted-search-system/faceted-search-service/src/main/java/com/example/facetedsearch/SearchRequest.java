package com.example.facetedsearch;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class SearchRequest {
    private Map<String, String> filters = new HashMap<>();
    private String textQuery;
    private Long fromTimestamp;  // Unix timestamp in milliseconds
    private Long toTimestamp;
    private int limit = 100;
    private int offset = 0;
}
