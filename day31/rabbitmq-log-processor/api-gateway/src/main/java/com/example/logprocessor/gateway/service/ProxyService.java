package com.example.logprocessor.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class ProxyService {

    private final RestTemplate restTemplate;
    private final String producerUrl;

    public ProxyService(
            RestTemplate restTemplate,
            @Value("${services.log-producer.url}") String producerUrl) {
        this.restTemplate = restTemplate;
        this.producerUrl = producerUrl;
    }

    public ResponseEntity<?> forwardToProducer(String path, Object body) {
        String url = producerUrl + path;
        log.debug("Forwarding request to: {}", url);
        return restTemplate.postForEntity(url, body, Object.class);
    }
}
