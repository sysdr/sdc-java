package com.example.logprocessor.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LogStatisticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogStatisticsService.class);
    
    private final LogEntryRepository repository;
    
    public LogStatisticsService(LogEntryRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Print statistics every 60 seconds
     */
    @Scheduled(fixedRate = 60000)
    public void printStatistics() {
        try {
            Instant oneMinuteAgo = Instant.now().minus(1, ChronoUnit.MINUTES);
            long recentCount = repository.countLogsSince(oneMinuteAgo);
            
            List<Object[]> countsByService = repository.countLogsByService();
            
            logger.info("=== Log Statistics (last 60s) ===");
            logger.info("Total logs processed: {}", recentCount);
            logger.info("Logs per second: {}", recentCount / 60.0);
            
            logger.info("Breakdown by service:");
            for (Object[] row : countsByService) {
                logger.info("  - {}: {} logs", row[0], row[1]);
            }
            logger.info("================================");
            
        } catch (Exception e) {
            logger.error("Failed to generate statistics", e);
        }
    }
}
