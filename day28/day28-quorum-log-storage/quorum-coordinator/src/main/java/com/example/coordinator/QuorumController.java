package com.example.coordinator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * REST API for quorum operations
 */
@RestController
@RequestMapping("/quorum")
@RequiredArgsConstructor
@Slf4j
public class QuorumController {
    
    private final QuorumService quorumService;

    @PostMapping("/write")
    public Mono<ResponseEntity<QuorumWriteResult>> write(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(defaultValue = "QUORUM") ConsistencyLevel consistency) {
        
        log.info("Quorum write request: key={}, consistency={}", key, consistency);
        
        return quorumService.write(key, value, consistency)
            .map(result -> result.isSuccess() 
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(503).body(result)
            )
            .onErrorResume(error -> {
                log.error("Quorum write error: {}", error.getMessage(), error);
                QuorumWriteResult errorResult = new QuorumWriteResult(false, 0, 0, null);
                return Mono.just(ResponseEntity.status(500).body(errorResult));
            });
    }

    @GetMapping("/read")
    public Mono<ResponseEntity<QuorumReadResult>> read(
            @RequestParam String key,
            @RequestParam(defaultValue = "QUORUM") ConsistencyLevel consistency) {
        
        log.info("Quorum read request: key={}, consistency={}", key, consistency);
        
        return quorumService.read(key, consistency)
            .map(result -> result.isSuccess()
                ? ResponseEntity.ok(result)
                : ResponseEntity.notFound().build()
            );
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy"));
    }
}
