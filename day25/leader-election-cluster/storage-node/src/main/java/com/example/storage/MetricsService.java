package com.example.storage;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    
    public void incrementElections() {
        Counter.builder("raft.elections.total")
            .description("Total number of leader elections")
            .register(meterRegistry)
            .increment();
    }
    
    public void recordLeaderElection(String nodeId) {
        Counter.builder("raft.leader.elections")
            .tag("node", nodeId)
            .description("Leader elections by node")
            .register(meterRegistry)
            .increment();
    }
    
    public void recordHeartbeat(int successful, int total) {
        meterRegistry.gauge("raft.heartbeat.success", successful);
        meterRegistry.gauge("raft.heartbeat.total", total);
    }
    
    public void recordWrite() {
        Counter.builder("raft.writes.total")
            .description("Total writes processed")
            .register(meterRegistry)
            .increment();
    }
}
