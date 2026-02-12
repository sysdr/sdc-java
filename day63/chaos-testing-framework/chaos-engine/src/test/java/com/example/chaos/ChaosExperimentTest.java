package com.example.chaos;

import com.example.chaos.engine.ChaosOrchestrator;
import com.example.chaos.model.ChaosExperiment;
import com.example.chaos.model.ExperimentResult;
import com.example.chaos.model.SteadyStateHypothesis;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ChaosExperimentTest {

    @Autowired
    private ChaosOrchestrator orchestrator;

    @Test
    void testProducerKillExperiment() {
        // Create experiment
        ChaosExperiment experiment = new ChaosExperiment();
        experiment.setName("Producer Service Kill Test");
        experiment.setDescription("Verify system resilience when producer is killed");
        experiment.setTargets(List.of("log-producer"));
        experiment.setFailureType(ChaosExperiment.FailureType.SERVICE_KILL);
        experiment.setDuration(Duration.ofSeconds(10));
        
        // Define hypothesis
        SteadyStateHypothesis hypothesis = new SteadyStateHypothesis();
        hypothesis.setTitle("System should recover within 30 seconds");
        hypothesis.setMaxRecoveryTimeSeconds(30);
        hypothesis.setMaxLatencyP95Ms(5000);
        experiment.setHypothesis(hypothesis);
        
        // Run experiment
        ExperimentResult result = orchestrator.runExperiment(experiment);
        
        // Assert
        assertNotNull(result);
        System.out.println("Experiment: " + result.getExperimentName());
        System.out.println("Success: " + result.isSuccess());
        result.getObservations().forEach(System.out::println);
        
        if (!result.isSuccess()) {
            System.out.println("Failure reason: " + result.getFailureReason());
        }
    }

    @Test
    void testNetworkLatencyExperiment() {
        ChaosExperiment experiment = new ChaosExperiment();
        experiment.setName("Network Latency Test");
        experiment.setDescription("Inject network latency to Kafka broker");
        experiment.setTargets(List.of("kafka"));
        experiment.setFailureType(ChaosExperiment.FailureType.NETWORK_LATENCY);
        experiment.setDuration(Duration.ofSeconds(15));
        
        SteadyStateHypothesis hypothesis = new SteadyStateHypothesis();
        hypothesis.setMaxLatencyP95Ms(2000);
        hypothesis.setMaxRecoveryTimeSeconds(20);
        experiment.setHypothesis(hypothesis);
        
        ExperimentResult result = orchestrator.runExperiment(experiment);
        
        assertNotNull(result);
        System.out.println("\n=== Network Latency Test ===");
        System.out.println("Success: " + result.isSuccess());
        result.getObservations().forEach(System.out::println);
    }
}
