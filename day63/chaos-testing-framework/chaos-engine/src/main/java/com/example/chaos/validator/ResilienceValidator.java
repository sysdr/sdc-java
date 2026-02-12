package com.example.chaos.validator;

import com.example.chaos.model.ChaosExperiment;
import com.example.chaos.model.ExperimentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Component
public class ResilienceValidator {
    private static final Logger logger = LoggerFactory.getLogger(ResilienceValidator.class);
    
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean validateSteadyState(ChaosExperiment experiment) {
        logger.info("Validating steady state for: {}", experiment.getName());
        
        try {
            // Check producer health
            String producerHealth = restTemplate.getForObject(
                "http://localhost:8081/api/logs/health", String.class);
            
            // Check consumer health
            String consumerHealth = restTemplate.getForObject(
                "http://localhost:8082/actuator/health", String.class);
            
            logger.info("Steady state validated: Producer={}, Consumer={}", 
                producerHealth != null, consumerHealth != null);
            
            return producerHealth != null && consumerHealth != null;
            
        } catch (Exception e) {
            logger.warn("Steady state validation failed: {}", e.getMessage());
            return false;
        }
    }

    public ExperimentResult validateResilience(ChaosExperiment experiment) {
        ExperimentResult result = new ExperimentResult();
        result.setExperimentName(experiment.getName());
        result.setStartTime(Instant.now());
        
        try {
            // Test request during chaos
            long startTime = System.currentTimeMillis();
            
            try {
                Map<String, Object> logData = Map.of(
                    "level", "INFO",
                    "message", "Chaos test message",
                    "source", "chaos-engine"
                );
                
                restTemplate.postForEntity(
                    "http://localhost:8080/gateway/logs", 
                    logData, 
                    Map.class);
                
                long latency = System.currentTimeMillis() - startTime;
                result.addObservation("Request latency: " + latency + "ms");
                
                // Validate against hypothesis
                if (experiment.getHypothesis() != null && 
                    latency < experiment.getHypothesis().getMaxLatencyP95Ms()) {
                    result.setSuccess(true);
                } else {
                    result.setFailureReason("Latency exceeded threshold");
                }
                
            } catch (Exception e) {
                result.addObservation("Request failed: " + e.getMessage());
                result.setSuccess(false);
                result.setFailureReason("Service unavailable during chaos");
            }
            
        } finally {
            result.setEndTime(Instant.now());
        }
        
        return result;
    }

    public boolean validateRecovery(ChaosExperiment experiment, long maxRecoveryTimeMs) {
        logger.info("Validating recovery for: {}", experiment.getName());
        
        long startTime = System.currentTimeMillis();
        long timeout = startTime + maxRecoveryTimeMs;
        
        while (System.currentTimeMillis() < timeout) {
            if (validateSteadyState(experiment)) {
                long recoveryTime = System.currentTimeMillis() - startTime;
                logger.info("Service recovered in {}ms", recoveryTime);
                return true;
            }
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        logger.error("Recovery validation timed out after {}ms", maxRecoveryTimeMs);
        return false;
    }
}
