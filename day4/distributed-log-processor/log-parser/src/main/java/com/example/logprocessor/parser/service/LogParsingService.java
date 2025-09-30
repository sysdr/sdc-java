package com.example.logprocessor.parser.service;

import com.example.logprocessor.parser.model.ParsedLogEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LogParsingService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogParsingService.class);
    
    // Apache Common Log Format pattern
    private static final Pattern APACHE_PATTERN = Pattern.compile(
        "^(\\S+) \\S+ \\S+ \\[([^\\]]+)\\] \"(\\S+) (\\S+) \\S+\" (\\d+) (\\d+|-).*$"
    );
    
    // Nginx Combined Log Format pattern with response time
    private static final Pattern NGINX_PATTERN = Pattern.compile(
        "^(\\S+) \\S+ \\S+ \\[([^\\]]+)\\] \"(\\S+) (\\S+) \\S+\" (\\d+) (\\d+|-) \"([^\"]*)\" \"([^\"]*)\"(?:\\s+rt=([\\d.]+))?.*$"
    );
    
    private static final DateTimeFormatter LOG_DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z");
    
    private final Counter parseSuccessCounter;
    private final Counter parseFailureCounter;
    private final Timer parseTimer;
    
    public LogParsingService(MeterRegistry meterRegistry) {
        this.parseSuccessCounter = Counter.builder("log_parse_success_total")
            .description("Total number of successfully parsed logs")
            .register(meterRegistry);
        this.parseFailureCounter = Counter.builder("log_parse_failure_total")
            .description("Total number of failed log parses")
            .register(meterRegistry);
        this.parseTimer = Timer.builder("log_parse_duration")
            .description("Time taken to parse logs")
            .register(meterRegistry);
    }
    
    public ParsedLogEvent parseLogEntry(String logEntry) {
        Timer.Sample sample = Timer.start();
        
        try {
            ParsedLogEvent event = null;
            
            // Try Apache format first
            Matcher apacheMatcher = APACHE_PATTERN.matcher(logEntry);
            if (apacheMatcher.matches()) {
                event = parseApacheLog(apacheMatcher, logEntry);
            } else {
                // Try Nginx format
                Matcher nginxMatcher = NGINX_PATTERN.matcher(logEntry);
                if (nginxMatcher.matches()) {
                    event = parseNginxLog(nginxMatcher, logEntry);
                }
            }
            
            if (event != null) {
                parseSuccessCounter.increment();
                logger.debug("Successfully parsed log: {}", event.getLogFormat());
                return event;
            } else {
                parseFailureCounter.increment();
                logger.warn("Failed to parse log entry: {}", logEntry);
                return createFailedParseEvent(logEntry, "Unknown format");
            }
            
        } catch (Exception e) {
            parseFailureCounter.increment();
            logger.error("Error parsing log entry: {}", logEntry, e);
            return createFailedParseEvent(logEntry, "Parse error: " + e.getMessage());
        } finally {
            sample.stop(parseTimer);
        }
    }
    
    private ParsedLogEvent parseApacheLog(Matcher matcher, String rawLog) {
        ParsedLogEvent event = new ParsedLogEvent(rawLog, "apache");
        
        try {
            event.setIpAddress(matcher.group(1));
            event.setTimestamp(parseTimestamp(matcher.group(2)));
            event.setHttpMethod(matcher.group(3));
            event.setRequestPath(matcher.group(4));
            event.setStatusCode(Integer.parseInt(matcher.group(5)));
            
            String sizeStr = matcher.group(6);
            if (!"-".equals(sizeStr)) {
                event.setResponseSize(Long.parseLong(sizeStr));
            }
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("parsed_at", LocalDateTime.now());
            metadata.put("parser_version", "1.0");
            event.setMetadata(metadata);
            
        } catch (Exception e) {
            logger.warn("Error parsing Apache log fields", e);
            throw e;
        }
        
        return event;
    }
    
    private ParsedLogEvent parseNginxLog(Matcher matcher, String rawLog) {
        ParsedLogEvent event = new ParsedLogEvent(rawLog, "nginx");
        
        try {
            event.setIpAddress(matcher.group(1));
            event.setTimestamp(parseTimestamp(matcher.group(2)));
            event.setHttpMethod(matcher.group(3));
            event.setRequestPath(matcher.group(4));
            event.setStatusCode(Integer.parseInt(matcher.group(5)));
            
            String sizeStr = matcher.group(6);
            if (!"-".equals(sizeStr)) {
                event.setResponseSize(Long.parseLong(sizeStr));
            }
            
            event.setReferer(matcher.group(7));
            event.setUserAgent(matcher.group(8));
            
            // Parse response time if available
            if (matcher.groupCount() >= 9 && matcher.group(9) != null) {
                event.setResponseTime(Double.parseDouble(matcher.group(9)));
            }
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("parsed_at", LocalDateTime.now());
            metadata.put("parser_version", "1.0");
            metadata.put("has_user_agent", event.getUserAgent() != null && !"-".equals(event.getUserAgent()));
            metadata.put("has_referer", event.getReferer() != null && !"-".equals(event.getReferer()));
            event.setMetadata(metadata);
            
        } catch (Exception e) {
            logger.warn("Error parsing Nginx log fields", e);
            throw e;
        }
        
        return event;
    }
    
    private LocalDateTime parseTimestamp(String timestampStr) {
        try {
            return LocalDateTime.parse(timestampStr, LOG_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse timestamp: {}", timestampStr);
            return LocalDateTime.now();
        }
    }
    
    private ParsedLogEvent createFailedParseEvent(String rawLog, String reason) {
        ParsedLogEvent event = new ParsedLogEvent(rawLog, "failed");
        event.setTimestamp(LocalDateTime.now());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("parse_failure_reason", reason);
        metadata.put("failed_at", LocalDateTime.now());
        event.setMetadata(metadata);
        
        return event;
    }
}
