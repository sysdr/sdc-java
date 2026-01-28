package com.example.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {
    
    private final SearchService searchService;
    
    @PostMapping
    public ResponseEntity<SearchResult> search(@RequestBody SearchRequest request) {
        log.info("Received search request: query='{}'", request.getQuery());
        SearchResult result = searchService.search(
            request.getQuery(), 
            request.getLimit(), 
            request.getWithScores()
        );
        return ResponseEntity.ok(result);
    }
    
    @GetMapping
    public ResponseEntity<SearchResult> searchGet(
            @RequestParam String query,
            @RequestParam(defaultValue = "100") Integer limit,
            @RequestParam(defaultValue = "false") Boolean withScores) {
        
        log.info("Received GET search request: query='{}'", query);
        SearchResult result = searchService.search(query, limit, withScores);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "search-api"));
    }
}
