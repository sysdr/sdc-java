package com.example.chaos.injector;

import com.example.chaos.model.ChaosExperiment;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DockerFailureInjector implements FailureInjector {
    private static final Logger logger = LoggerFactory.getLogger(DockerFailureInjector.class);
    
    private final DockerClient dockerClient;

    public DockerFailureInjector() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
            .build();
        
        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    @Override
    public void inject(ChaosExperiment experiment) throws Exception {
        logger.info("Injecting failure: {} on targets: {}", 
            experiment.getFailureType(), experiment.getTargets());
        
        switch (experiment.getFailureType()) {
            case SERVICE_KILL:
                killServices(experiment.getTargets());
                break;
            case NETWORK_LATENCY:
                injectNetworkLatency(experiment.getTargets());
                break;
            case RESOURCE_EXHAUSTION:
                exhaustResources(experiment.getTargets());
                break;
            default:
                logger.warn("Unsupported failure type: {}", experiment.getFailureType());
        }
    }

    @Override
    public void recover(ChaosExperiment experiment) throws Exception {
        logger.info("Recovering from failure: {} on targets: {}", 
            experiment.getFailureType(), experiment.getTargets());
        
        switch (experiment.getFailureType()) {
            case SERVICE_KILL:
                restartServices(experiment.getTargets());
                break;
            case NETWORK_LATENCY:
                removeNetworkLatency(experiment.getTargets());
                break;
            case RESOURCE_EXHAUSTION:
                releaseResources(experiment.getTargets());
                break;
            default:
                logger.warn("Unsupported recovery for type: {}", experiment.getFailureType());
        }
    }

    private void killServices(java.util.List<String> targets) {
        targets.forEach(containerName -> {
            try {
                String containerId = findContainerByName(containerName);
                if (containerId != null) {
                    dockerClient.killContainerCmd(containerId).exec();
                    logger.info("Killed container: {}", containerName);
                }
            } catch (Exception e) {
                logger.error("Failed to kill container: {}", containerName, e);
            }
        });
    }

    private void restartServices(java.util.List<String> targets) {
        targets.forEach(containerName -> {
            try {
                String containerId = findContainerByName(containerName);
                if (containerId != null) {
                    dockerClient.startContainerCmd(containerId).exec();
                    logger.info("Restarted container: {}", containerName);
                    
                    // Wait for health check
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                logger.error("Failed to restart container: {}", containerName, e);
            }
        });
    }

    private void injectNetworkLatency(java.util.List<String> targets) {
        targets.forEach(containerName -> {
            try {
                String containerId = findContainerByName(containerName);
                if (containerId != null) {
                    // Use tc (traffic control) to add latency
                    ExecCreateCmdResponse exec = dockerClient.execCreateCmd(containerId)
                        .withCmd("tc", "qdisc", "add", "dev", "eth0", "root", "netem", "delay", "100ms")
                        .withAttachStdout(false)
                        .withAttachStderr(false)
                        .exec();

                    dockerClient.execStartCmd(exec.getId())
                        .withDetach(true)
                        .exec(new ResultCallback.Adapter<>());
                    logger.info("Injected network latency on: {}", containerName);
                }
            } catch (Exception e) {
                logger.warn("Failed to inject latency (tc may not be available): {}", containerName);
            }
        });
    }

    private void removeNetworkLatency(java.util.List<String> targets) {
        targets.forEach(containerName -> {
            try {
                String containerId = findContainerByName(containerName);
                if (containerId != null) {
                    dockerClient.execCreateCmd(containerId)
                        .withAttachStdout(true)
                        .withAttachStderr(true)
                        .withCmd("sh", "-c", "tc qdisc del dev eth0 root")
                        .exec();
                    logger.info("Removed network latency from: {}", containerName);
                }
            } catch (Exception e) {
                logger.warn("Failed to remove latency: {}", containerName);
            }
        });
    }

    private void exhaustResources(java.util.List<String> targets) {
        targets.forEach(containerName -> {
            try {
                String containerId = findContainerByName(containerName);
                if (containerId != null) {
                    // Simulate memory pressure
                    ExecCreateCmdResponse exec = dockerClient.execCreateCmd(containerId)
                        .withCmd("sh", "-c", "stress-ng --vm 1 --vm-bytes 90% --timeout 60s &")
                        .withAttachStdout(false)
                        .withAttachStderr(false)
                        .exec();

                    dockerClient.execStartCmd(exec.getId())
                        .withDetach(true)
                        .exec(new ResultCallback.Adapter<>());
                    logger.info("Injected resource exhaustion on: {}", containerName);
                }
            } catch (Exception e) {
                logger.warn("Failed to exhaust resources (stress-ng may not be available): {}", containerName);
            }
        });
    }

    private void releaseResources(java.util.List<String> targets) {
        logger.info("Resources will auto-release after stress timeout");
    }

    private String findContainerByName(String name) {
        try {
            return dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .stream()
                .filter(container -> {
                    String[] names = container.getNames();
                    return names != null && java.util.Arrays.stream(names)
                        .anyMatch(n -> n.contains(name));
                })
                .findFirst()
                .map(com.github.dockerjava.api.model.Container::getId)
                .orElse(null);
        } catch (Exception e) {
            logger.error("Error finding container: {}", name, e);
            return null;
        }
    }
}
