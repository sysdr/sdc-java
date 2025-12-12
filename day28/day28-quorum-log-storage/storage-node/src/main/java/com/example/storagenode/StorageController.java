package com.example.storagenode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for storage node operations
 */
@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
@Slf4j
public class StorageController {
    
    private final StorageService storageService;

    @PostMapping("/write")
    public ResponseEntity<Map<String, Object>> write(@RequestBody WriteRequest request) {
        log.info("Write request: key={}", request.getKey());
        
        VersionVector resultVector = storageService.write(
            request.getKey(),
            request.getValue(),
            request.getVersion()
        );
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "version", resultVector.getVector()
        ));
    }

    @GetMapping("/read/{key}")
    public ResponseEntity<List<VersionedValue>> read(@PathVariable String key) {
        log.info("Read request: key={}", key);
        List<VersionedValue> values = storageService.read(key);
        return ResponseEntity.ok(values);
    }

    @GetMapping("/keys")
    public ResponseEntity<List<String>> getAllKeys() {
        return ResponseEntity.ok(storageService.getAllKeys());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(storageService.getStatus());
    }
}
