package com.example.logprocessor.normalizer.handler;

import com.example.logprocessor.common.format.LogFormat;
import com.example.logprocessor.common.model.CanonicalLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class TextFormatHandler implements FormatHandler {

    // Common log patterns
    private static final Pattern STANDARD_LOG_PATTERN = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}[.,]?\\d*Z?)\\s+" +
        "\\[?(\\w+)\\]?\\s+" +
        "\\[?([\\w.-]+)\\]?\\s*" +
        "(?:\\[([\\w-]+)\\]\\s*)?" +
        "(.*)$"
    );

    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile(
        "(\\w+)=([\"']?)([^\"'\\s]+)\\2"
    );

    @Override
    public LogFormat getFormat() {
        return LogFormat.TEXT;
    }

    @Override
    public CanonicalLog parse(byte[] input) throws FormatParseException {
        try {
            String text = new String(input, StandardCharsets.UTF_8).trim();
            
            Matcher matcher = STANDARD_LOG_PATTERN.matcher(text);
            if (matcher.matches()) {
                Map<String, String> metadata = extractKeyValues(text);
                
                return CanonicalLog.builder()
                        .id(UUID.randomUUID().toString())
                        .timestamp(parseTimestamp(matcher.group(1)))
                        .level(CanonicalLog.LogLevel.fromString(matcher.group(2)))
                        .service(matcher.group(3))
                        .host(matcher.group(4) != null ? matcher.group(4) : "unknown")
                        .message(matcher.group(5))
                        .metadata(metadata)
                        .rawContent(text)
                        .build();
            }

            // Fallback: treat entire text as message
            return CanonicalLog.builder()
                    .id(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .level(CanonicalLog.LogLevel.INFO)
                    .service("unknown")
                    .host("unknown")
                    .message(text)
                    .metadata(new HashMap<>())
                    .rawContent(text)
                    .build();

        } catch (Exception e) {
            throw new FormatParseException("Failed to parse text: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] serialize(CanonicalLog log) throws FormatSerializeException {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(DateTimeFormatter.ISO_INSTANT.format(log.getTimestamp()));
            sb.append(" [").append(log.getLevel()).append("]");
            sb.append(" [").append(log.getService()).append("]");
            if (log.getHost() != null && !log.getHost().equals("unknown")) {
                sb.append(" [").append(log.getHost()).append("]");
            }
            sb.append(" ").append(log.getMessage());

            if (log.getMetadata() != null && !log.getMetadata().isEmpty()) {
                log.getMetadata().forEach((k, v) -> 
                    sb.append(" ").append(k).append("=").append(v));
            }

            return sb.toString().getBytes(StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new FormatSerializeException("Failed to serialize to text: " + e.getMessage(), e);
        }
    }

    private Instant parseTimestamp(String ts) {
        try {
            return Instant.parse(ts);
        } catch (DateTimeParseException e) {
            try {
                return Instant.parse(ts.replace(" ", "T") + "Z");
            } catch (DateTimeParseException e2) {
                return Instant.now();
            }
        }
    }

    private Map<String, String> extractKeyValues(String text) {
        Map<String, String> result = new HashMap<>();
        Matcher matcher = KEY_VALUE_PATTERN.matcher(text);
        while (matcher.find()) {
            result.put(matcher.group(1), matcher.group(3));
        }
        return result;
    }
}
