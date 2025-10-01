package com.example.logprocessor.consumer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class RotationPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(RotationPolicyService.class);

    @Value("${log.rotation.max-file-size:100MB}")
    private String maxFileSize;

    @Value("${log.rotation.max-age-hours:24}")
    private int maxAgeHours;

    private final FileStorageService fileStorageService;

    @Autowired
    public RotationPolicyService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void evaluateRotationPolicies() {
        logger.debug("Evaluating rotation policies");
        
        try {
            checkSizeBasedRotation();
            checkTimeBasedRotation();
        } catch (Exception e) {
            logger.error("Error during rotation policy evaluation", e);
        }
    }

    private void checkSizeBasedRotation() {
        long maxSizeBytes = parseFileSize(maxFileSize);
        File coldStorageDir = new File("logs/cold/");
        
        if (coldStorageDir.exists() && coldStorageDir.isDirectory()) {
            File[] files = coldStorageDir.listFiles((dir, name) -> name.endsWith(".log"));
            
            if (files != null) {
                for (File file : files) {
                    if (file.length() > maxSizeBytes) {
                        rotateFile(file);
                    }
                }
            }
        }
    }

    private void checkTimeBasedRotation() {
        File coldStorageDir = new File("logs/cold/");
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(maxAgeHours);
        
        if (coldStorageDir.exists() && coldStorageDir.isDirectory()) {
            File[] files = coldStorageDir.listFiles((dir, name) -> name.endsWith(".log"));
            
            if (files != null) {
                for (File file : files) {
                    LocalDateTime fileModified = LocalDateTime.ofEpochSecond(
                        file.lastModified() / 1000, 0, java.time.ZoneOffset.UTC);
                    
                    if (fileModified.isBefore(cutoffTime)) {
                        rotateFile(file);
                    }
                }
            }
        }
    }

    private void rotateFile(File file) {
        String originalName = file.getName();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String rotatedName = originalName.replace(".log", "-" + timestamp + ".log.gz");
        
        boolean success = fileStorageService.rotateFile(originalName, rotatedName);
        if (success) {
            logger.info("Rotated file: {} -> {}", originalName, rotatedName);
        } else {
            logger.warn("Failed to rotate file: {}", originalName);
        }
    }

    private long parseFileSize(String sizeStr) {
        if (sizeStr.endsWith("KB")) {
            return Long.parseLong(sizeStr.replace("KB", "")) * 1024;
        } else if (sizeStr.endsWith("MB")) {
            return Long.parseLong(sizeStr.replace("MB", "")) * 1024 * 1024;
        } else if (sizeStr.endsWith("GB")) {
            return Long.parseLong(sizeStr.replace("GB", "")) * 1024 * 1024 * 1024;
        } else {
            return Long.parseLong(sizeStr); // Assume bytes
        }
    }
}
