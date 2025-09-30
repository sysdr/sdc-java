package com.example.logprocessor.producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LogGeneratorService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogGeneratorService.class);
    private final AtomicLong requestCounter = new AtomicLong(0);
    private final Random random = new Random();
    
    @Autowired
    private KafkaProducerService kafkaProducerService;
    
    private final String[] ips = {
        "192.168.1.100", "10.0.0.15", "172.16.0.200", "203.0.113.45",
        "198.51.100.78", "8.8.8.8", "1.1.1.1", "192.168.0.50"
    };
    
    private final String[] userAgents = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    };
    
    private final String[] paths = {
        "/api/users", "/api/orders", "/health", "/metrics", "/login", "/dashboard", 
        "/api/products", "/static/css/main.css", "/static/js/app.js", "/favicon.ico"
    };
    
    private final int[] statusCodes = {200, 200, 200, 200, 201, 404, 500, 302, 401};
    
    @Scheduled(fixedDelay = 1000) // Generate logs every second
    public void generateApacheLogs() {
        for (int i = 0; i < random.nextInt(5) + 1; i++) {
            String logEntry = generateApacheLogEntry();
            kafkaProducerService.sendRawLog(logEntry);
        }
    }
    
    @Scheduled(fixedDelay = 1500) // Generate Nginx logs every 1.5 seconds
    public void generateNginxLogs() {
        for (int i = 0; i < random.nextInt(3) + 1; i++) {
            String logEntry = generateNginxLogEntry();
            kafkaProducerService.sendRawLog(logEntry);
        }
    }
    
    private String generateApacheLogEntry() {
        // Apache Common Log Format: IP - - [timestamp] "method path HTTP/1.1" status size
        String ip = ips[random.nextInt(ips.length)];
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss +0000"));
        String method = random.nextBoolean() ? "GET" : "POST";
        String path = paths[random.nextInt(paths.length)];
        int status = statusCodes[random.nextInt(statusCodes.length)];
        int size = random.nextInt(10000) + 100;
        
        return String.format("%s - - [%s] \"%s %s HTTP/1.1\" %d %d",
            ip, timestamp, method, path, status, size);
    }
    
    private String generateNginxLogEntry() {
        // Nginx combined log format with additional fields
        String ip = ips[random.nextInt(ips.length)];
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss +0000"));
        String method = random.nextBoolean() ? "GET" : "POST";
        String path = paths[random.nextInt(paths.length)];
        int status = statusCodes[random.nextInt(statusCodes.length)];
        int size = random.nextInt(10000) + 100;
        String userAgent = userAgents[random.nextInt(userAgents.length)];
        String referer = random.nextBoolean() ? "-" : "https://example.com/";
        double responseTime = Math.round(random.nextDouble() * 2.0 * 1000.0) / 1000.0;
        
        return String.format("%s - - [%s] \"%s %s HTTP/1.1\" %d %d \"%s\" \"%s\" rt=%.3f",
            ip, timestamp, method, path, status, size, referer, userAgent, responseTime);
    }
}
