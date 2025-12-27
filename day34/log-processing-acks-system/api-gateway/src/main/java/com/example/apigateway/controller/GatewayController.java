package com.example.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
@Slf4j
public class GatewayController {

    @Value("${producer.url}")
    private String producerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/logs")
    public ResponseEntity<String> forwardToProducer(@RequestBody String logEvent) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(logEvent, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                producerUrl + "/api/logs",
                request,
                String.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            log.error("Failed to forward request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to process request");
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Gateway is healthy");
    }
}
