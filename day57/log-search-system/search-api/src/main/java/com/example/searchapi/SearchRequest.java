package com.example.searchapi;

import lombok.Data;

import java.util.List;

@Data
public class SearchRequest {
    private String query;
    private List<String> severities;
    private List<String> serviceNames;
    private String startTime;
    private String endTime;
    private int page = 0;
    private int size = 20;
}
