package com.example.logshipper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
public class LogController {
    
    private final LogBatchingService batchingService;
    
    @PostMapping
    public ResponseEntity<Void> receiveLog(@RequestBody LogEvent event) {
        batchingService.addLog(event);
        return ResponseEntity.accepted().build();
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
