package com.example.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class GatewayController {

    private final RestTemplate restTemplate;

    @Value("${services.producer.url}")
    private String producerUrl;

    @Value("${services.search.url}")
    private String searchUrl;

    @PostMapping("/logs/generate")
    public ResponseEntity<?> generateLogs(@RequestParam(defaultValue = "100") int count) {
        String url = producerUrl + "/api/logs/generate?count=" + count;
        return restTemplate.postForEntity(url, null, String.class);
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody Object request) {
        String url = searchUrl + "/api/search";
        return restTemplate.postForEntity(url, request, Object.class);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Gateway is healthy");
    }
}
