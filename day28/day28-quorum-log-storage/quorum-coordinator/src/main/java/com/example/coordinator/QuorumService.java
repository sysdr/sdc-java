package com.example.coordinator;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Quorum coordinator implementing W+R>N consistency protocol
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QuorumService {
    
    private final WebClient.Builder webClientBuilder;
    private final QuorumProperties quorumProperties;
    
    private List<String> getReplicaUrls() {
        return quorumProperties.getReplicas();
    }
    
    private long getTimeoutMs() {
        return quorumProperties.getTimeout().getMs();
    }

    /**
     * Perform quorum write: send to all replicas, wait for W acknowledgments
     */
    public Mono<QuorumWriteResult> write(String key, String value, ConsistencyLevel consistencyLevel) {
        List<String> replicaUrls = getReplicaUrls();
        int requiredAcks = consistencyLevel.getRequiredReplicas(replicaUrls.size());
        
        log.info("Quorum write: key={}, consistency={}, required={}/{}", 
                 key, consistencyLevel, requiredAcks, replicaUrls.size());

        AtomicInteger successCount = new AtomicInteger(0);
        List<Map<String, Object>> responses = new ArrayList<>();
        
        return Flux.fromIterable(replicaUrls)
            .flatMap(replicaUrl -> writeToReplica(replicaUrl, key, value)
                .doOnSuccess(response -> {
                    successCount.incrementAndGet();
                    synchronized (responses) {
                        responses.add(response);
                    }
                })
                .onErrorResume(error -> {
                    log.warn("Write failed to replica {}: {}", replicaUrl, error.getMessage());
                    return Mono.empty();
                })
                .timeout(Duration.ofMillis(getTimeoutMs()), Mono.empty())
            )
            .timeout(Duration.ofMillis(getTimeoutMs() * 3))
            .collectList()
            .map(results -> {
                boolean success = successCount.get() >= requiredAcks;
                
                // Extract version vector from successful writes (merge all)
                VersionVector finalVersion = responses.stream()
                    .map(resp -> (Map<String, Long>) resp.get("version"))
                    .filter(Objects::nonNull)
                    .map(VersionVector::new)
                    .reduce(VersionVector::merge)
                    .orElse(new VersionVector());

                log.info("Quorum write complete: success={}, acks={}/{}", 
                         success, successCount.get(), requiredAcks);
                
                return new QuorumWriteResult(
                    success,
                    successCount.get(),
                    requiredAcks,
                    finalVersion
                );
            });
    }

    /**
     * Perform quorum read: read from R replicas, resolve conflicts
     */
    public Mono<QuorumReadResult> read(String key, ConsistencyLevel consistencyLevel) {
        List<String> replicaUrls = getReplicaUrls();
        int requiredReads = consistencyLevel.getRequiredReplicas(replicaUrls.size());
        
        log.info("Quorum read: key={}, consistency={}, required={}/{}", 
                 key, consistencyLevel, requiredReads, replicaUrls.size());

        return Flux.fromIterable(replicaUrls.subList(0, Math.min(requiredReads, replicaUrls.size())))
            .flatMap(replicaUrl -> readFromReplica(replicaUrl, key)
                .onErrorResume(error -> {
                    log.warn("Read failed from replica {}: {}", replicaUrl, error.getMessage());
                    return Mono.empty();
                })
            )
            .timeout(Duration.ofMillis(getTimeoutMs()))
            .collectList()
            .map(allValues -> {
                // Flatten all versioned values from all replicas
                List<VersionedValue> allVersionedValues = allValues.stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

                if (allVersionedValues.isEmpty()) {
                    return new QuorumReadResult(null, false, Collections.emptyList());
                }

                // Resolve conflicts using version vector comparison
                ResolvedValue resolved = resolveConflicts(allVersionedValues);
                
                log.info("Quorum read complete: key={}, conflicts={}", 
                         key, resolved.getConflictingValues().size());
                
                return new QuorumReadResult(
                    resolved.getLatestValue(),
                    true,
                    resolved.getConflictingValues()
                );
            });
    }

    @CircuitBreaker(name = "storageNode", fallbackMethod = "writeFallback")
    private Mono<Map<String, Object>> writeToReplica(String replicaUrl, String key, String value) {
        WebClient client = webClientBuilder.baseUrl(replicaUrl).build();
        
        Map<String, Object> request = Map.of(
            "key", key,
            "value", value,
            "version", new HashMap<String, Long>()
        );
        
        return client.post()
            .uri("/storage/write")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .timeout(Duration.ofMillis(getTimeoutMs()));
    }

    @CircuitBreaker(name = "storageNode", fallbackMethod = "readFallback")
    private Mono<List<VersionedValue>> readFromReplica(String replicaUrl, String key) {
        WebClient client = webClientBuilder.baseUrl(replicaUrl).build();
        
        return client.get()
            .uri("/storage/read/{key}", key)
            .retrieve()
            .bodyToFlux(VersionedValue.class)
            .collectList()
            .timeout(Duration.ofMillis(getTimeoutMs()));
    }

    /**
     * Resolve conflicts between multiple versions using version vectors
     */
    private ResolvedValue resolveConflicts(List<VersionedValue> values) {
        if (values.isEmpty()) {
            return new ResolvedValue(null, Collections.emptyList());
        }

        if (values.size() == 1) {
            return new ResolvedValue(values.get(0), Collections.emptyList());
        }

        // Find values that are not dominated by any other value
        List<VersionedValue> nonDominated = new ArrayList<>();
        
        for (VersionedValue candidate : values) {
            boolean isDominated = false;
            
            for (VersionedValue other : values) {
                if (candidate != other && 
                    other.getVersion().compareTo(candidate.getVersion()) > 0) {
                    isDominated = true;
                    break;
                }
            }
            
            if (!isDominated) {
                nonDominated.add(candidate);
            }
        }

        if (nonDominated.size() == 1) {
            // Single latest value, no conflicts
            return new ResolvedValue(nonDominated.get(0), Collections.emptyList());
        } else {
            // Multiple concurrent versions - conflicts detected
            // Return the one with latest timestamp as "latest", others as conflicts
            VersionedValue latest = nonDominated.stream()
                .max(Comparator.comparing(VersionedValue::getTimestamp))
                .orElse(nonDominated.get(0));
            
            List<VersionedValue> conflicts = nonDominated.stream()
                .filter(v -> v != latest)
                .collect(Collectors.toList());
            
            return new ResolvedValue(latest, conflicts);
        }
    }

    // Fallback methods for circuit breaker
    private Mono<Map<String, Object>> writeFallback(String replicaUrl, String key, String value, Exception ex) {
        log.error("Write fallback for {}: {}", replicaUrl, ex.getMessage());
        return Mono.empty();
    }

    private Mono<List<VersionedValue>> readFallback(String replicaUrl, String key, Exception ex) {
        log.error("Read fallback for {}: {}", replicaUrl, ex.getMessage());
        return Mono.empty();
    }
}

@lombok.Data
@lombok.AllArgsConstructor
class ResolvedValue {
    private VersionedValue latestValue;
    private List<VersionedValue> conflictingValues;
}
