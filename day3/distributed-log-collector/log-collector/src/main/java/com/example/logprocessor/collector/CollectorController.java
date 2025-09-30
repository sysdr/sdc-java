package com.example.logprocessor.collector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CollectorController {
    
    @Autowired
    private LogCollectorService logCollectorService;
    
    @Autowired
    private KafkaProducerService kafkaProducerService;

    @GetMapping("/")
    public Map<String, Object> welcome() {
        return Map.of(
            "service", "Log Collector Service",
            "status", "running",
            "endpoints", Map.of(
                "stats", "/api/collector/stats",
                "health", "/actuator/health"
            )
        );
    }

    @GetMapping("/api/collector/stats")
    public Map<String, Object> getStats() {
        return Map.of(
            "processedEvents", logCollectorService.getProcessedEventsCount(),
            "skippedDuplicates", logCollectorService.getSkippedDuplicatesCount(),
            "sentToKafka", kafkaProducerService.getSentEventsCount(),
            "kafkaFailures", kafkaProducerService.getFailedEventsCount(),
            "status", "running"
        );
    }
}
