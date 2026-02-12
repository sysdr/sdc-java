package com.example.chaos.engine;

import com.example.chaos.injector.FailureInjector;
import com.example.chaos.model.ChaosExperiment;
import com.example.chaos.model.ExperimentResult;
import com.example.chaos.validator.ResilienceValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ChaosOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(ChaosOrchestrator.class);
    
    private final FailureInjector failureInjector;
    private final ResilienceValidator validator;

    public ChaosOrchestrator(FailureInjector failureInjector, ResilienceValidator validator) {
        this.failureInjector = failureInjector;
        this.validator = validator;
    }

    public ExperimentResult runExperiment(ChaosExperiment experiment) {
        logger.info("Starting chaos experiment: {}", experiment.getName());
        
        ExperimentResult result = new ExperimentResult();
        result.setExperimentName(experiment.getName());
        result.setStartTime(Instant.now());
        
        try {
            // Step 1: Validate steady state
            if (!validator.validateSteadyState(experiment)) {
                result.setSuccess(false);
                result.setFailureReason("System not in steady state before experiment");
                return result;
            }
            result.addObservation("Pre-chaos steady state: VALIDATED");
            
            // Step 2: Inject failure
            failureInjector.inject(experiment);
            result.addObservation("Failure injected: " + experiment.getFailureType());
            
            // Step 3: Observe system behavior during chaos
            Thread.sleep(experiment.getDuration().toMillis());
            ExperimentResult chaosResult = validator.validateResilience(experiment);
            result.getObservations().addAll(chaosResult.getObservations());
            
            // Step 4: Recover
            failureInjector.recover(experiment);
            result.addObservation("Recovery initiated");
            
            // Step 5: Validate recovery
            int maxRecoverySeconds = experiment.getHypothesis() != null ? 
                experiment.getHypothesis().getMaxRecoveryTimeSeconds() : 60;
            
            boolean recovered = validator.validateRecovery(experiment, maxRecoverySeconds * 1000L);
            
            if (recovered) {
                result.setSuccess(true);
                result.addObservation("Post-chaos steady state: VALIDATED");
            } else {
                result.setSuccess(false);
                result.setFailureReason("System did not recover within " + maxRecoverySeconds + "s");
            }
            
        } catch (Exception e) {
            logger.error("Experiment failed with exception", e);
            result.setSuccess(false);
            result.setFailureReason("Exception: " + e.getMessage());
            
            // Attempt recovery
            try {
                failureInjector.recover(experiment);
            } catch (Exception recoveryEx) {
                logger.error("Recovery also failed", recoveryEx);
            }
            
        } finally {
            result.setEndTime(Instant.now());
        }
        
        logger.info("Experiment completed: {} - Success: {}", 
            experiment.getName(), result.isSuccess());
        
        return result;
    }
}
