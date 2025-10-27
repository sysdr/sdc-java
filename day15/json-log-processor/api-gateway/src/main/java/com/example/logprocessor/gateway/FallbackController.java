package com.example.logprocessor.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {
    
    @PostMapping("/logs")
    public ResponseEntity<Map<String, String>> logFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "unavailable");
        response.put("message", "Log processing service is currently unavailable. Please try again later.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
