package com.example.indexnode;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class IndexController {

    private final IndexService indexService;

    public IndexController(IndexService indexService) {
        this.indexService = indexService;
    }

    @PostMapping("/index")
    public ResponseEntity<String> index(@RequestBody LogEntry logEntry) {
        indexService.indexLog(logEntry);
        return ResponseEntity.ok("Indexed: " + logEntry.getLogId());
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResult> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "100") int limit) {
        SearchResult result = indexService.search(q, limit);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<IndexService.IndexStats> stats() {
        return ResponseEntity.ok(indexService.getStats());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
