package com.example.producer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    private Long id;
    private String level;
    private String service;
    private String message;
    private String timestamp;
    private String userId;
    private String traceId;
    
    public static LogEvent generate(Long id) {
        LogEvent event = new LogEvent();
        event.setId(id);
        event.setTimestamp(Instant.now().toString());
        event.setLevel(randomLevel());
        event.setService(randomService());
        event.setMessage(randomMessage(event.getLevel()));
        event.setUserId("user-" + (1000 + (id % 5000)));
        event.setTraceId("trace-" + id);
        return event;
    }
    
    private static String randomLevel() {
        String[] levels = {"INFO", "WARN", "ERROR", "DEBUG"};
        return levels[(int)(Math.random() * levels.length)];
    }
    
    private static String randomService() {
        String[] services = {"auth-service", "payment-service", "user-service", 
                           "order-service", "notification-service"};
        return services[(int)(Math.random() * services.length)];
    }
    
    private static String randomMessage(String level) {
        switch(level) {
            case "ERROR":
                return randomError();
            case "WARN":
                return randomWarning();
            case "INFO":
                return randomInfo();
            default:
                return randomDebug();
        }
    }
    
    private static String randomError() {
        String[] errors = {
            "Database connection timeout after 30 seconds",
            "Authentication failed for user session",
            "Payment processing failed: insufficient funds",
            "Order validation error: invalid product ID",
            "Network timeout connecting to external API",
            "Cache miss followed by database read failure",
            "Rate limit exceeded for API endpoint",
            "Invalid JWT token signature verification",
            "Deadlock detected in transaction processing"
        };
        return errors[(int)(Math.random() * errors.length)];
    }
    
    private static String randomWarning() {
        String[] warnings = {
            "High memory usage detected: 85% threshold exceeded",
            "Slow query detected: execution time 2.5 seconds",
            "Cache hit rate below 60% threshold",
            "Connection pool size approaching maximum",
            "Retry attempt 3 of 5 for failed operation",
            "Deprecated API endpoint usage detected",
            "SSL certificate expiring in 30 days"
        };
        return warnings[(int)(Math.random() * warnings.length)];
    }
    
    private static String randomInfo() {
        String[] infos = {
            "User login successful from IP address",
            "Order processed successfully with confirmation",
            "Payment completed for transaction amount",
            "User profile updated successfully",
            "Notification sent via email channel",
            "Cache refreshed with latest data",
            "Health check passed for all dependencies"
        };
        return infos[(int)(Math.random() * infos.length)];
    }
    
    private static String randomDebug() {
        String[] debugs = {
            "Processing request with correlation ID",
            "Database query executed in 45 milliseconds",
            "Cache lookup performed for key",
            "Validation passed for input parameters",
            "Response serialized to JSON format"
        };
        return debugs[(int)(Math.random() * debugs.length)];
    }
}
