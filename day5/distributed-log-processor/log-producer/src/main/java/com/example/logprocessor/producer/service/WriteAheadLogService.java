package com.example.logprocessor.producer.service;

import com.example.logprocessor.producer.model.LogEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class WriteAheadLogService {

    private static final Logger logger = LoggerFactory.getLogger(WriteAheadLogService.class);
    private final ObjectMapper objectMapper;
    private final String walDirectory;

    public WriteAheadLogService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.walDirectory = "logs/wal/";
        
        // Create WAL directory if it doesn't exist
        File dir = new File(walDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void append(LogEvent logEvent) {
        try {
            String fileName = generateWalFileName();
            String jsonMessage = objectMapper.writeValueAsString(logEvent);
            
            synchronized (this) {
                try (FileWriter writer = new FileWriter(walDirectory + fileName, true)) {
                    writer.write(jsonMessage + "\n");
                    writer.flush();
                }
            }
            
            logger.debug("Successfully appended log event to WAL: trace_id={}", logEvent.getTraceId());
            
        } catch (IOException e) {
            logger.error("Failed to write log event to WAL: trace_id={}", logEvent.getTraceId(), e);
            throw new RuntimeException("Failed to write to WAL", e);
        }
    }

    private String generateWalFileName() {
        LocalDateTime now = LocalDateTime.now();
        String dateHour = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));
        return "wal-" + dateHour + ".log";
    }
}
