package com.example.router;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class RouterController {

    private final RoutingService routingService;
    private final ConsistentHashRing hashRing;

    public RouterController(RoutingService routingService, ConsistentHashRing hashRing) {
        this.routingService = routingService;
        this.hashRing = hashRing;
    }

    @PostMapping("/route")
    public Mono<ResponseEntity<String>> route(@RequestBody LogEntry logEntry) {
        return routingService.routeLog(logEntry)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.internalServerError()
                                .body("Routing failed: " + e.getMessage())
                ));
    }

    @GetMapping("/ring/distribution")
    public ResponseEntity<Map<String, Integer>> getDistribution(
            @RequestParam(defaultValue = "10000") int samples) {
        Map<String, Integer> distribution = hashRing.getNodeDistribution(samples);
        return ResponseEntity.ok(distribution);
    }

    @GetMapping("/ring/nodes")
    public ResponseEntity<?> getNodes() {
        return ResponseEntity.ok(hashRing.getPhysicalNodes());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
