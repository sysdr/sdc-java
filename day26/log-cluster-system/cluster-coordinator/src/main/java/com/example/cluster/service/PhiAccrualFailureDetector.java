package com.example.cluster.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;

/**
 * Implementation of Phi Accrual Failure Detector algorithm
 * Used by Cassandra and Akka for adaptive failure detection
 */
@Component
public class PhiAccrualFailureDetector {
    private static final int WINDOW_SIZE = 100;
    private static final double PHI_THRESHOLD = 8.0;
    
    private final ConcurrentHashMap<String, LinkedList<Long>> arrivalIntervals;
    
    public PhiAccrualFailureDetector() {
        this.arrivalIntervals = new ConcurrentHashMap<>();
    }
    
    public void recordHeartbeat(String nodeId) {
        arrivalIntervals.computeIfAbsent(nodeId, k -> new LinkedList<>());
        LinkedList<Long> intervals = arrivalIntervals.get(nodeId);
        
        long now = System.currentTimeMillis();
        if (!intervals.isEmpty()) {
            long lastTime = intervals.getLast();
            intervals.add(now - lastTime);
            
            if (intervals.size() > WINDOW_SIZE) {
                intervals.removeFirst();
            }
        }
        intervals.add(now);
    }
    
    public double phi(long timeSinceLastHeartbeat) {
        if (timeSinceLastHeartbeat <= 0) return 0.0;
        
        // Simplified phi calculation
        // In production, this would use mean and variance from historical intervals
        double expectedInterval = 1000.0; // 1 second heartbeat interval
        double phi = timeSinceLastHeartbeat / expectedInterval;
        
        return Math.log10(phi);
    }
    
    public boolean isAvailable(String nodeId, long timeSinceLastHeartbeat) {
        return phi(timeSinceLastHeartbeat) < PHI_THRESHOLD;
    }
}
