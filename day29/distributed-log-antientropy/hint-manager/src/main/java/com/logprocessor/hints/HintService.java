package com.logprocessor.hints;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class HintService {
    
    @Autowired
    private HintRepository hintRepository;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Counter hintsStored;
    private final Counter hintsDelivered;
    private final Counter hintsExpired;
    
    @Value("${hint.expiry.hours:3}")
    private int expiryHours;
    
    @Value("${hint.max.retries:3}")
    private int maxRetries;
    
    public HintService(WebClient.Builder webClientBuilder, 
                      MeterRegistry meterRegistry,
                      HintRepository hintRepository) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
        this.hintsStored = Counter.builder("hints_stored_total")
            .description("Total hints stored")
            .register(meterRegistry);
        this.hintsDelivered = Counter.builder("hints_delivered_total")
            .description("Total hints delivered")
            .register(meterRegistry);
        this.hintsExpired = Counter.builder("hints_expired_total")
            .description("Total hints expired")
            .register(meterRegistry);
        
        // Register gauge for pending hints
        Gauge.builder("hints_pending", hintRepository, 
            r -> r.findByStatusOrderByCreatedAt("PENDING").size())
            .description("Current pending hints count")
            .register(meterRegistry);
    }
    
    public void storeHint(String targetNodeUrl, String partitionId, Map<String, Object> writeData) {
        try {
            Hint hint = new Hint();
            hint.setTargetNodeUrl(targetNodeUrl);
            hint.setPartitionId(partitionId);
            hint.setPayload(objectMapper.writeValueAsString(writeData));
            hint.setCreatedAt(Instant.now());
            hint.setStatus("PENDING");
            hint.setRetryCount(0);
            
            hintRepository.save(hint);
            hintsStored.increment();
        } catch (Exception e) {
            throw new RuntimeException("Failed to store hint", e);
        }
    }
    
    @Scheduled(fixedDelay = 5000) // Check every 5 seconds
    public void deliverHints() {
        List<Hint> pendingHints = hintRepository.findByStatusOrderByCreatedAt("PENDING");
        
        for (Hint hint : pendingHints) {
            if (hint.getRetryCount() >= maxRetries) {
                hint.setStatus("EXPIRED");
                hintRepository.save(hint);
                hintsExpired.increment();
                continue;
            }
            
            if (isNodeHealthy(hint.getTargetNodeUrl())) {
                deliverHint(hint);
            }
        }
    }
    
    @Scheduled(fixedDelay = 60000) // Check every minute
    public void expireOldHints() {
        Instant expiryTime = Instant.now().minus(expiryHours, ChronoUnit.HOURS);
        List<Hint> expiredHints = hintRepository.findExpiredHints(expiryTime);
        
        for (Hint hint : expiredHints) {
            hint.setStatus("EXPIRED");
            hintRepository.save(hint);
            hintsExpired.increment();
        }
    }
    
    private boolean isNodeHealthy(String nodeUrl) {
        try {
            Map<String, Object> response = webClient.get()
                .uri(nodeUrl + "/api/storage/health")
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            return "UP".equals(response.get("status"));
        } catch (Exception e) {
            return false;
        }
    }
    
    private void deliverHint(Hint hint) {
        try {
            Map<String, Object> writeData = objectMapper.readValue(hint.getPayload(), Map.class);
            
            webClient.post()
                .uri(hint.getTargetNodeUrl() + "/api/storage/write")
                .body(BodyInserters.fromValue(writeData))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            hint.setStatus("DELIVERED");
            hint.setDeliveredAt(Instant.now());
            hintRepository.save(hint);
            hintsDelivered.increment();
            
        } catch (Exception e) {
            hint.setRetryCount(hint.getRetryCount() + 1);
            hintRepository.save(hint);
        }
    }
}
