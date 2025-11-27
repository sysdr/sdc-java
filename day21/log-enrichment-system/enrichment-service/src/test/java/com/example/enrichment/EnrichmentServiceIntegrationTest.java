package com.example.enrichment;

import com.example.enrichment.model.EnrichedLogEvent;
import com.example.enrichment.model.LogEvent;
import com.example.enrichment.service.EnrichmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EnrichmentServiceIntegrationTest {
    
    @Autowired
    private EnrichmentService enrichmentService;
    
    @Test
    void testBasicEnrichment() {
        LogEvent logEvent = LogEvent.builder()
            .id("test-001")
            .timestamp(Instant.now())
            .level("INFO")
            .message("Test log message")
            .service("test-service")
            .sourceIp("192.168.1.100")
            .logSchemaVersion("v2.0")
            .build();
        
        EnrichedLogEvent enriched = enrichmentService.enrich(logEvent);
        
        assertNotNull(enriched);
        assertNotNull(enriched.getOriginalEvent());
        assertNotNull(enriched.getEnrichment());
        assertEquals("test-001", enriched.getOriginalEvent().getId());
    }
}
