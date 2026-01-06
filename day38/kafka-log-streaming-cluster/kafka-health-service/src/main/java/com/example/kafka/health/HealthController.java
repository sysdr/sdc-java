package com.example.kafka.health;

import com.example.kafka.models.ClusterHealth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private ClusterHealthMonitor healthMonitor;

    @GetMapping
    public ResponseEntity<ClusterHealth> getClusterHealth() {
        ClusterHealth health = healthMonitor.getLatestHealth();
        return ResponseEntity.ok(health);
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        ClusterHealth health = healthMonitor.getLatestHealth();
        return ResponseEntity.ok(health.getStatus());
    }
}
