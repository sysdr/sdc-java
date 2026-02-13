package com.example.query.service;

import com.example.query.model.EncryptedField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class DecryptionClient {
    
    private final WebClient encryptionServiceClient;
    
    public String decryptField(EncryptedField encryptedField) {
        try {
            return encryptionServiceClient.post()
                .uri("/api/encryption/decrypt")
                .body(Mono.just(encryptedField), EncryptedField.class)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (Exception e) {
            log.error("Failed to decrypt field: {}", encryptedField.getFieldName(), e);
            return "[DECRYPTION_FAILED]";
        }
    }
}
