package com.example.logprocessor.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LogGeneratorService {
    private static final Logger logger = LoggerFactory.getLogger(LogGeneratorService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    @Value("${log-generator.output-directory:/tmp/logs}")
    private String outputDirectory;
    
    @Value("${log-generator.events-per-second:10}")
    private int eventsPerSecond;
    
    private final Random random = new Random();
    private final AtomicLong eventCounter = new AtomicLong(0);
    
    private final List<String> logLevels = List.of("INFO", "WARN", "ERROR", "DEBUG");
    private final List<String> services = List.of("user-service", "payment-service", "order-service", "inventory-service");
    private final List<String> operations = List.of("process_payment", "create_user", "update_inventory", "place_order");

    @Scheduled(fixedRateString = "#{${log-generator.events-per-second:10} > 0 ? 1000 / ${log-generator.events-per-second:10} : 1000}")
    public void generateLogEntry() {
        try {
            String logEntry = createLogEntry();
            writeToFile(logEntry);
            eventCounter.incrementAndGet();
        } catch (Exception e) {
            logger.error("Failed to generate log entry", e);
        }
    }

    private String createLogEntry() {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String level = logLevels.get(random.nextInt(logLevels.size()));
        String service = services.get(random.nextInt(services.size()));
        String operation = operations.get(random.nextInt(operations.size()));
        long requestId = 1000000 + (long)(random.nextDouble() * (9999999 - 1000000));
        long userId = 1000 + (long)(random.nextDouble() * (99999 - 1000));
        int responseTime = 50 + random.nextInt(2000 - 50);
        
        return String.format("[%s] %s [%s] [req:%d] [user:%d] Operation '%s' completed in %dms",
                timestamp, level, service, requestId, userId, operation, responseTime);
    }

    private void writeToFile(String logEntry) throws IOException {
        Path logDir = Paths.get(outputDirectory);
        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
        }
        
        Path logFile = logDir.resolve("application.log");
        Files.write(logFile, (logEntry + System.lineSeparator()).getBytes(), 
                   StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public long getGeneratedEventCount() {
        return eventCounter.get();
    }
}
