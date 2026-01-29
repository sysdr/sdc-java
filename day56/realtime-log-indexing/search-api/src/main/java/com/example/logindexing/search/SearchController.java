package com.example.logindexing.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@Slf4j
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        log.info("Search request: {}", request);
        SearchResponse response = searchService.search(request);
        log.info("Search completed in {}ms with {} results", 
            response.getQueryTimeMs(), response.getTotalResults());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<SearchResponse> searchGet(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "100") int limit) {
        
        SearchRequest request = new SearchRequest();
        request.setQuery(query);
        request.setLevel(level);
        request.setService(service);
        request.setUserId(userId);
        request.setLimit(limit);
        
        return search(request);
    }
}
