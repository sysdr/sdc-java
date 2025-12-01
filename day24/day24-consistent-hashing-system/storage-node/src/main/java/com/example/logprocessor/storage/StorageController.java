package com.example.logprocessor.storage;

import com.example.logprocessor.common.LogEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {
    
    private final StorageService storageService;
    
    @PostMapping("/store")
    public ResponseEntity<Void> storeLog(@RequestBody LogEvent event) {
        storageService.storeLog(event);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/query")
    public ResponseEntity<List<LogEvent>> queryBySourceIp(
            @RequestParam String sourceIp) {
        return ResponseEntity.ok(storageService.queryBySourceIp(sourceIp));
    }
    
    @GetMapping("/query/range")
    public ResponseEntity<List<LogEvent>> queryByTimeRange(
            @RequestParam String sourceIp,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        return ResponseEntity.ok(
            storageService.queryBySourceIpAndTimeRange(sourceIp, start, end));
    }
    
    @GetMapping("/count")
    public ResponseEntity<Long> getLogCount() {
        return ResponseEntity.ok(storageService.getLogCount());
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Storage node healthy");
    }
}
