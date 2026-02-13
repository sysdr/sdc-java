package com.example.encryption.service;

import com.example.encryption.model.EncryptionKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Manages encryption key lifecycle with automatic rotation.
 * Keys are cached in Redis with versioning for zero-downtime rotation.
 */
@Service
@Slf4j
public class KeyManagementService {
    
    private static final String KEY_CACHE_PREFIX = "encryption:key:";
    private static final String CURRENT_KEY_VERSION = "encryption:current:version";
    private static final int KEY_VALIDITY_DAYS = 30;
    private static final int KEY_OVERLAP_HOURS = 24;
    
    private final RedisTemplate<String, EncryptionKey> encryptionKeyRedisTemplate;
    private final RedisTemplate<String, Integer> integerRedisTemplate;
    private final SecureRandom secureRandom;
    
    public KeyManagementService(RedisTemplate<String, EncryptionKey> encryptionKeyRedisTemplate,
                               RedisTemplate<String, Integer> integerRedisTemplate) {
        this.encryptionKeyRedisTemplate = encryptionKeyRedisTemplate;
        this.integerRedisTemplate = integerRedisTemplate;
        this.secureRandom = new SecureRandom();
    }
    
    @PostConstruct
    public void init() {
        initializeKeyIfNeeded();
    }
    
    /**
     * Get current active encryption key with caching.
     */
    public EncryptionKey getCurrentKey() {
        Integer currentVersion = integerRedisTemplate.opsForValue()
            .get(CURRENT_KEY_VERSION);
        
        if (currentVersion == null) {
            return generateAndCacheNewKey(1);
        }
        
        String keyId = KEY_CACHE_PREFIX + currentVersion;
        EncryptionKey key = encryptionKeyRedisTemplate.opsForValue().get(keyId);
        
        if (key == null || key.getValidUntil().isBefore(Instant.now())) {
            return generateAndCacheNewKey(currentVersion + 1);
        }
        
        return key;
    }
    
    /**
     * Get specific key version for decryption of old data.
     */
    public EncryptionKey getKeyByVersion(int version) {
        String keyId = KEY_CACHE_PREFIX + version;
        EncryptionKey key = encryptionKeyRedisTemplate.opsForValue().get(keyId);
        
        if (key == null) {
            throw new IllegalStateException("Encryption key version " + version + " not found");
        }
        
        return key;
    }
    
    /**
     * Scheduled key rotation - runs daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void rotateKeysIfNeeded() {
        log.info("Checking if key rotation is needed...");
        
        EncryptionKey currentKey = getCurrentKey();
        Instant rotationThreshold = Instant.now().plus(KEY_OVERLAP_HOURS, ChronoUnit.HOURS);
        
        if (currentKey.getValidUntil().isBefore(rotationThreshold)) {
            log.info("Rotating encryption key from version {} to {}", 
                currentKey.getVersion(), currentKey.getVersion() + 1);
            generateAndCacheNewKey(currentKey.getVersion() + 1);
        }
    }
    
    private void initializeKeyIfNeeded() {
        Integer currentVersion = integerRedisTemplate.opsForValue()
            .get(CURRENT_KEY_VERSION);
        
        if (currentVersion == null) {
            log.info("Initializing first encryption key...");
            generateAndCacheNewKey(1);
        }
    }
    
    private EncryptionKey generateAndCacheNewKey(int version) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, secureRandom);
            SecretKey secretKey = keyGen.generateKey();
            
            Instant now = Instant.now();
            EncryptionKey encryptionKey = new EncryptionKey(
                "encryption-key-v" + version,
                "AES-256-GCM",
                secretKey.getEncoded(),
                now,
                now.plus(KEY_VALIDITY_DAYS, ChronoUnit.DAYS),
                version
            );
            
            // Cache key with extended TTL to allow decryption of old data
            String keyId = KEY_CACHE_PREFIX + version;
            encryptionKeyRedisTemplate.opsForValue().set(keyId, encryptionKey, 
                KEY_VALIDITY_DAYS + 30, TimeUnit.DAYS);
            
            // Update current version pointer
            integerRedisTemplate.opsForValue().set(CURRENT_KEY_VERSION, version);
            
            log.info("Generated and cached new encryption key: {}", encryptionKey.getKeyId());
            return encryptionKey;
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }
}
