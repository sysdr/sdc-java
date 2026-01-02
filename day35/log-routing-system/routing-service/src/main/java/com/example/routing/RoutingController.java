package com.example.routing;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
public class RoutingController {
    
    private final RoutingRuleManager ruleManager;
    private final KafkaProducerService producerService;
    private final Counter logsReceived;
    
    public RoutingController(RoutingRuleManager ruleManager,
                            KafkaProducerService producerService,
                            MeterRegistry meterRegistry) {
        this.ruleManager = ruleManager;
        this.producerService = producerService;
        this.logsReceived = Counter.builder("logs.received")
            .description("Number of logs received")
            .register(meterRegistry);
    }
    
    @PostMapping("/logs")
    public ResponseEntity<Map<String, String>> ingestLog(@RequestBody LogEvent event) {
        try {
            logsReceived.increment();
            
            // Evaluate routing rules
            List<String> destinations = ruleManager.evaluateRouting(event);
            
            // Route to Kafka topics
            producerService.routeLog(event, destinations);
            
            return ResponseEntity.accepted()
                .body(Map.of(
                    "id", event.getId(),
                    "destinations", String.join(",", destinations)
                ));
        } catch (Exception e) {
            log.error("Error processing log", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/rules")
    public ResponseEntity<List<RoutingRule>> getRules() {
        return ResponseEntity.ok(ruleManager.getRules());
    }
}
