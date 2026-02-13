package com.example.consumer.service;

import com.example.consumer.model.EncryptedField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EncryptionClient {
    
    private final WebClient encryptionServiceClient;
    
    public List<EncryptedField> encryptFields(Map<String, String> fields) {
        try {
            EncryptionRequest request = new EncryptionRequest();
            request.setFields(fields);
            request.setClassification("PII");
            
            EncryptionResponse response = encryptionServiceClient.post()
                .uri("/api/encryption/encrypt")
                .body(Mono.just(request), EncryptionRequest.class)
                .retrieve()
                .bodyToMono(EncryptionResponse.class)
                .block();
            
            return response != null ? response.getEncryptedFields() : List.of();
            
        } catch (Exception e) {
            log.error("Failed to encrypt fields", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    static class EncryptionRequest {
        private Map<String, String> fields;
        private String classification;
        
        public Map<String, String> getFields() { return fields; }
        public void setFields(Map<String, String> fields) { this.fields = fields; }
        public String getClassification() { return classification; }
        public void setClassification(String classification) { this.classification = classification; }
    }
    
    static class EncryptionResponse {
        private List<EncryptedField> encryptedFields;
        private long processingTimeMs;
        
        public List<EncryptedField> getEncryptedFields() { return encryptedFields; }
        public void setEncryptedFields(List<EncryptedField> encryptedFields) { 
            this.encryptedFields = encryptedFields; 
        }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { 
            this.processingTimeMs = processingTimeMs; 
        }
    }
}
