package com.example.coordinator;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CoordinatorController {

    private final ScatterGatherService scatterGatherService;

    public CoordinatorController(ScatterGatherService scatterGatherService) {
        this.scatterGatherService = scatterGatherService;
    }

    @GetMapping("/search")
    public ResponseEntity<MergedSearchResult> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "100") int limit) {
        MergedSearchResult result = scatterGatherService.search(q, limit);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
