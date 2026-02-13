package com.example.encryption.service;

import com.example.encryption.model.EncryptedField;
import com.example.encryption.model.EncryptionKey;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Field-level encryption service using AES-256-GCM.
 * Supports both regular encryption and deterministic searchable encryption via HMAC.
 */
@Service
@Slf4j
public class FieldEncryptionService {
    
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    
    private final KeyManagementService keyManagementService;
    private final SecureRandom secureRandom;
    private final Timer encryptionTimer;
    private final Timer decryptionTimer;
    private final Counter encryptionCounter;
    private final Counter decryptionCounter;
    
    public FieldEncryptionService(KeyManagementService keyManagementService,
                                 MeterRegistry meterRegistry) {
        this.keyManagementService = keyManagementService;
        this.secureRandom = new SecureRandom();
        
        this.encryptionTimer = Timer.builder("encryption.operation.duration")
            .description("Time taken for encryption operations")
            .register(meterRegistry);
        
        this.decryptionTimer = Timer.builder("decryption.operation.duration")
            .description("Time taken for decryption operations")
            .register(meterRegistry);
        
        this.encryptionCounter = Counter.builder("encryption.operations.total")
            .description("Total encryption operations")
            .register(meterRegistry);
        
        this.decryptionCounter = Counter.builder("decryption.operations.total")
            .description("Total decryption operations")
            .register(meterRegistry);
    }
    
    /**
     * Encrypt a field value with current key.
     * Uses AES-256-GCM for authenticated encryption.
     */
    public EncryptedField encrypt(String fieldName, String plaintext) {
        return encryptionTimer.record(() -> {
            try {
                encryptionCounter.increment();
                
                EncryptionKey key = keyManagementService.getCurrentKey();
                
                // Generate random IV for GCM mode
                byte[] iv = new byte[GCM_IV_LENGTH];
                secureRandom.nextBytes(iv);
                
                // Perform AES-GCM encryption
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                SecretKey secretKey = new SecretKeySpec(key.getKey(), "AES");
                GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
                
                byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
                
                // Generate searchable HMAC hash
                String hmacHash = generateHmacHash(plaintext, key.getKey());
                
                return new EncryptedField(
                    fieldName,
                    Base64.getEncoder().encodeToString(encrypted),
                    key.getKeyId(),
                    Base64.getEncoder().encodeToString(iv),
                    hmacHash
                );
                
            } catch (Exception e) {
                log.error("Encryption failed for field: {}", fieldName, e);
                throw new RuntimeException("Encryption failed", e);
            }
        });
    }
    
    /**
     * Decrypt an encrypted field using the specified key version.
     */
    public String decrypt(EncryptedField encryptedField) {
        return decryptionTimer.record(() -> {
            try {
                decryptionCounter.increment();
                
                // Extract key version from keyId
                int version = extractKeyVersion(encryptedField.getKeyId());
                EncryptionKey key = keyManagementService.getKeyByVersion(version);
                
                byte[] iv = Base64.getDecoder().decode(encryptedField.getIv());
                byte[] encrypted = Base64.getDecoder().decode(encryptedField.getEncryptedValue());
                
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                SecretKey secretKey = new SecretKeySpec(key.getKey(), "AES");
                GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
                
                byte[] decrypted = cipher.doFinal(encrypted);
                return new String(decrypted, StandardCharsets.UTF_8);
                
            } catch (Exception e) {
                log.error("Decryption failed for field: {}", encryptedField.getFieldName(), e);
                throw new RuntimeException("Decryption failed", e);
            }
        });
    }
    
    /**
     * Generate deterministic HMAC hash for searchable encryption.
     */
    private String generateHmacHash(String value, byte[] keyBytes) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKey hmacKey = new SecretKeySpec(keyBytes, "HmacSHA256");
            mac.init(hmacKey);
            byte[] hash = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC generation failed", e);
        }
    }
    
    private int extractKeyVersion(String keyId) {
        // Extract version from keyId format: "encryption-key-v123"
        return Integer.parseInt(keyId.substring(keyId.lastIndexOf('v') + 1));
    }
}
