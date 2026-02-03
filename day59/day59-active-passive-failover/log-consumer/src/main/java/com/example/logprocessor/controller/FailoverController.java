package com.example.logprocessor.controller;

import com.example.logprocessor.service.FailoverCoordinator;
import com.example.logprocessor.service.StatefulLogConsumer;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/failover")
public class FailoverController {
    
    private final FailoverCoordinator failoverCoordinator;
    private final StatefulLogConsumer logConsumer;
    
    public FailoverController(
            FailoverCoordinator failoverCoordinator,
            StatefulLogConsumer logConsumer) {
        this.failoverCoordinator = failoverCoordinator;
        this.logConsumer = logConsumer;
    }
    
    @GetMapping("/status")
    public FailoverStatus getStatus() {
        return new FailoverStatus(
            failoverCoordinator.getInstanceId(),
            failoverCoordinator.hasLeadership(),
            failoverCoordinator.getCurrentEpoch(),
            logConsumer.getMessagesProcessed(),
            logConsumer.isActive()
        );
    }
    
    @Data
    @AllArgsConstructor
    public static class FailoverStatus {
        private String instanceId;
        private boolean isLeader;
        private long epoch;
        private long messagesProcessed;
        private boolean isActive;
    }
}
