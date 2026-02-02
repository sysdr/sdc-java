package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
class SearchIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testSearchEndpoint() {
        webTestClient.get()
                .uri("/api/v1/logs/search?service=checkout&level=ERROR&timeRange=1h&limit=10")
                .header("X-API-Key", "test-key")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.logs").exists()
                .jsonPath("$.totalHits").exists();
    }

    @Test
    void testRateLimiting() {
        // Make requests until rate limit is hit
        for (int i = 0; i < 1100; i++) {
            webTestClient.get()
                    .uri("/api/v1/logs/search?service=test&limit=1")
                    .header("X-API-Key", "standard-key")
                    .exchange();
        }

        // Next request should be rate limited
        webTestClient.get()
                .uri("/api/v1/logs/search?service=test&limit=1")
                .header("X-API-Key", "standard-key")
                .exchange()
                .expectStatus().isEqualTo(429);
    }
}
