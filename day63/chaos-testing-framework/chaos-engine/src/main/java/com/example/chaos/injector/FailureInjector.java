package com.example.chaos.injector;

import com.example.chaos.model.ChaosExperiment;

public interface FailureInjector {
    void inject(ChaosExperiment experiment) throws Exception;
    void recover(ChaosExperiment experiment) throws Exception;
}
