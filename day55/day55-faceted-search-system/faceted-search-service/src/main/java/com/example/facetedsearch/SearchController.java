package com.example.facetedsearch;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final FacetedSearchService searchService;

    @PostMapping
    public ResponseEntity<FacetedSearchResponse> search(@RequestBody SearchRequest request) {
        FacetedSearchResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Search service is healthy");
    }
}
