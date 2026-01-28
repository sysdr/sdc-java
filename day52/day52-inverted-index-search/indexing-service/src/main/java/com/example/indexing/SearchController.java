package com.example.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/index")
@RequiredArgsConstructor
@Slf4j
public class SearchController {
    
    private final InvertedIndex invertedIndex;
    
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "false") boolean withScores) {
        
        log.info("Search request: query='{}', withScores={}", query, withScores);
        
        if (withScores) {
            List<ScoredDocument> results = invertedIndex.searchWithScores(query, 1000);
            return ResponseEntity.ok(Map.of(
                "query", query,
                "results", results,
                "totalResults", results.size()
            ));
        } else {
            var results = invertedIndex.search(query);
            List<Map<String, Object>> formattedResults = results.stream()
                .map(docId -> Map.of("docId", (Object)docId))
                .toList();
            
            return ResponseEntity.ok(Map.of(
                "query", query,
                "results", formattedResults,
                "totalResults", results.size()
            ));
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(invertedIndex.getIndexStats());
    }
    
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy", 
            "service", "indexing-service",
            "indexSize", invertedIndex.getIndexSize(),
            "documentCount", invertedIndex.getDocumentCount()
        ));
    }
}
