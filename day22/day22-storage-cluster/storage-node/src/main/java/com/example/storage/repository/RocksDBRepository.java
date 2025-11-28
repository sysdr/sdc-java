package com.example.storage.repository;

import com.example.storage.model.LogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Repository
public class RocksDBRepository {
    
    static {
        RocksDB.loadLibrary();
    }
    
    private RocksDB db;
    private final ObjectMapper objectMapper;
    
    public RocksDBRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    @Value("${storage.rocksdb.path:./data/rocksdb}")
    private String dbPath;
    
    @PostConstruct
    public void init() {
        try {
            Options options = new Options();
            options.setCreateIfMissing(true);
            options.setMaxOpenFiles(100);
            options.setWriteBufferSize(64 * 1024 * 1024); // 64MB
            
            db = RocksDB.open(options, dbPath);
            log.info("RocksDB initialized at path: {}", dbPath);
        } catch (RocksDBException e) {
            log.error("Failed to initialize RocksDB", e);
            throw new RuntimeException("Could not initialize RocksDB", e);
        }
    }
    
    public void put(String key, LogEntry entry) {
        try {
            byte[] valueBytes = objectMapper.writeValueAsBytes(entry);
            db.put(key.getBytes(StandardCharsets.UTF_8), valueBytes);
            log.debug("Stored log entry with key: {}", key);
        } catch (Exception e) {
            log.error("Failed to store entry for key: {}", key, e);
            throw new RuntimeException("Failed to store entry", e);
        }
    }
    
    public Optional<LogEntry> get(String key) {
        try {
            byte[] valueBytes = db.get(key.getBytes(StandardCharsets.UTF_8));
            if (valueBytes == null) {
                return Optional.empty();
            }
            LogEntry entry = objectMapper.readValue(valueBytes, LogEntry.class);
            return Optional.of(entry);
        } catch (Exception e) {
            log.error("Failed to retrieve entry for key: {}", key, e);
            return Optional.empty();
        }
    }
    
    public void delete(String key) {
        try {
            db.delete(key.getBytes(StandardCharsets.UTF_8));
            log.debug("Deleted log entry with key: {}", key);
        } catch (RocksDBException e) {
            log.error("Failed to delete entry for key: {}", key, e);
            throw new RuntimeException("Failed to delete entry", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (db != null) {
            db.close();
            log.info("RocksDB closed");
        }
    }
}
