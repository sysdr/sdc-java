package com.example.logprocessor.consumer.service;

import com.example.logprocessor.consumer.model.LogEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private final ObjectMapper objectMapper;
    private final String coldStorageDirectory;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public FileStorageService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.coldStorageDirectory = "logs/cold/";
        
        // Create cold storage directory if it doesn't exist
        File dir = new File(coldStorageDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void storeLogEvent(LogEvent logEvent) {
        lock.writeLock().lock();
        try {
            String fileName = generateFileName(logEvent);
            String jsonMessage = objectMapper.writeValueAsString(logEvent);
            
            try (FileWriter writer = new FileWriter(coldStorageDirectory + fileName, true)) {
                writer.write(jsonMessage + "\n");
                writer.flush();
            }
            
            logger.debug("Successfully stored log event in cold storage: trace_id={}", logEvent.getTraceId());
            
        } catch (IOException e) {
            logger.error("Failed to write log event to cold storage: trace_id={}", logEvent.getTraceId(), e);
            throw new RuntimeException("Failed to write to cold storage", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private String generateFileName(LogEvent logEvent) {
        String date = logEvent.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return logEvent.getLevel().toLowerCase() + "-" + date + ".log";
    }

    public long getFileSize(String fileName) {
        File file = new File(coldStorageDirectory + fileName);
        return file.exists() ? file.length() : 0;
    }

    public boolean rotateFile(String currentFileName, String newFileName) {
        lock.writeLock().lock();
        try {
            File currentFile = new File(coldStorageDirectory + currentFileName);
            File newFile = new File(coldStorageDirectory + newFileName);
            
            if (currentFile.exists()) {
                boolean success = currentFile.renameTo(newFile);
                if (success) {
                    logger.info("Successfully rotated file: {} -> {}", currentFileName, newFileName);
                } else {
                    logger.error("Failed to rotate file: {} -> {}", currentFileName, newFileName);
                }
                return success;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
