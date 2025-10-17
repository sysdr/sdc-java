package com.example.logprocessor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EndToEndTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testLogIngestionWithTLS() {
        Map<String, Object> logEvent = Map.of(
            "level", "INFO",
            "message", "End-to-end test with TLS",
            "source", "integration-test",
            "metadata", Map.of("test", true)
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/logs",
            logEvent,
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsKey("id");
    }
}
