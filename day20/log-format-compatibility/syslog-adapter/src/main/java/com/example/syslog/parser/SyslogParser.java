package com.example.syslog.parser;

import com.example.syslog.model.SyslogMessage;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SyslogParser {
    
    // RFC 3164 pattern: <pri>timestamp hostname tag: message
    private static final Pattern RFC3164_PATTERN = Pattern.compile(
        "^<(\\d+)>(\\w{3}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+(\\S+)\\s+(\\S+):\\s*(.*)$"
    );
    
    // RFC 5424 pattern: <pri>version timestamp hostname app-name procid msgid [structured-data] message
    private static final Pattern RFC5424_PATTERN = Pattern.compile(
        "^<(\\d+)>(\\d+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\[.*?\\]|-)\\s*(.*)$"
    );

    private static final DateTimeFormatter RFC3164_FORMATTER = 
        DateTimeFormatter.ofPattern("MMM dd HH:mm:ss");

    private static final Map<Integer, String> FACILITY_MAP = new HashMap<>();
    private static final Map<Integer, String> SEVERITY_MAP = new HashMap<>();

    static {
        FACILITY_MAP.put(0, "kern");
        FACILITY_MAP.put(1, "user");
        FACILITY_MAP.put(2, "mail");
        FACILITY_MAP.put(3, "daemon");
        FACILITY_MAP.put(4, "auth");
        FACILITY_MAP.put(5, "syslog");
        FACILITY_MAP.put(16, "local0");
        FACILITY_MAP.put(23, "local7");

        SEVERITY_MAP.put(0, "EMERGENCY");
        SEVERITY_MAP.put(1, "ALERT");
        SEVERITY_MAP.put(2, "CRITICAL");
        SEVERITY_MAP.put(3, "ERROR");
        SEVERITY_MAP.put(4, "WARNING");
        SEVERITY_MAP.put(5, "NOTICE");
        SEVERITY_MAP.put(6, "INFO");
        SEVERITY_MAP.put(7, "DEBUG");
    }

    public SyslogMessage parse(String rawMessage, String sourceIp) {
        // Try RFC 5424 first
        Matcher matcher5424 = RFC5424_PATTERN.matcher(rawMessage);
        if (matcher5424.matches()) {
            return parseRFC5424(matcher5424, rawMessage, sourceIp);
        }

        // Fall back to RFC 3164
        Matcher matcher3164 = RFC3164_PATTERN.matcher(rawMessage);
        if (matcher3164.matches()) {
            return parseRFC3164(matcher3164, rawMessage, sourceIp);
        }

        // If no match, create basic message
        return createBasicMessage(rawMessage, sourceIp);
    }

    private SyslogMessage parseRFC3164(Matcher matcher, String rawMessage, String sourceIp) {
        int priority = Integer.parseInt(matcher.group(1));
        String timestamp = matcher.group(2);
        String hostname = matcher.group(3);
        String appName = matcher.group(4);
        String message = matcher.group(5);

        int facility = priority / 8;
        int severity = priority % 8;

        SyslogMessage syslogMsg = new SyslogMessage();
        syslogMsg.setFacility(FACILITY_MAP.getOrDefault(facility, "unknown"));
        syslogMsg.setSeverity(SEVERITY_MAP.getOrDefault(severity, "INFO"));
        syslogMsg.setTimestamp(parseRFC3164Timestamp(timestamp));
        syslogMsg.setHostname(hostname);
        syslogMsg.setAppName(appName);
        syslogMsg.setMessage(message);
        syslogMsg.setProtocol("RFC3164");
        syslogMsg.setSourceIp(sourceIp);
        syslogMsg.setRawMessage(rawMessage);

        return syslogMsg;
    }

    private SyslogMessage parseRFC5424(Matcher matcher, String rawMessage, String sourceIp) {
        int priority = Integer.parseInt(matcher.group(1));
        String version = matcher.group(2);
        String timestamp = matcher.group(3);
        String hostname = matcher.group(4);
        String appName = matcher.group(5);
        String procId = matcher.group(6);
        String msgId = matcher.group(7);
        String structuredData = matcher.group(8);
        String message = matcher.group(9);

        int facility = priority / 8;
        int severity = priority % 8;

        SyslogMessage syslogMsg = new SyslogMessage();
        syslogMsg.setFacility(FACILITY_MAP.getOrDefault(facility, "unknown"));
        syslogMsg.setSeverity(SEVERITY_MAP.getOrDefault(severity, "INFO"));
        syslogMsg.setTimestamp(parseISO8601Timestamp(timestamp));
        syslogMsg.setHostname(hostname.equals("-") ? null : hostname);
        syslogMsg.setAppName(appName.equals("-") ? null : appName);
        syslogMsg.setProcId(procId.equals("-") ? null : procId);
        syslogMsg.setMsgId(msgId.equals("-") ? null : msgId);
        syslogMsg.setMessage(message);
        syslogMsg.setProtocol("RFC5424");
        syslogMsg.setSourceIp(sourceIp);
        syslogMsg.setRawMessage(rawMessage);

        if (!structuredData.equals("-")) {
            syslogMsg.setStructuredData(parseStructuredData(structuredData));
        }

        return syslogMsg;
    }

    private SyslogMessage createBasicMessage(String rawMessage, String sourceIp) {
        SyslogMessage msg = new SyslogMessage();
        msg.setMessage(rawMessage);
        msg.setTimestamp(Instant.now());
        msg.setSeverity("INFO");
        msg.setProtocol("UNPARSED");
        msg.setSourceIp(sourceIp);
        msg.setRawMessage(rawMessage);
        return msg;
    }

    private Instant parseRFC3164Timestamp(String timestamp) {
        try {
            LocalDateTime ldt = LocalDateTime.parse(
                timestamp, 
                RFC3164_FORMATTER.withZone(ZoneId.systemDefault())
            );
            return ldt.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private Instant parseISO8601Timestamp(String timestamp) {
        try {
            return Instant.parse(timestamp);
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private Map<String, String> parseStructuredData(String data) {
        Map<String, String> result = new HashMap<>();
        // Simplified parsing - production would be more robust
        Pattern pattern = Pattern.compile("(\\w+)=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(data);
        while (matcher.find()) {
            result.put(matcher.group(1), matcher.group(2));
        }
        return result;
    }
}
