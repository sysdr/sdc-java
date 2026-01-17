package com.example.logproducer;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProducerController {

    private final LogEventGenerator logEventGenerator;

    public ProducerController(LogEventGenerator logEventGenerator) {
        this.logEventGenerator = logEventGenerator;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return Map.of(
            "totalEventsProduced", logEventGenerator.getEventCount(),
            "status", "running"
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
