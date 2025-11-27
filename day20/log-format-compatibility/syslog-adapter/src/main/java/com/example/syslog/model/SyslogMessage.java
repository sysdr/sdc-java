package com.example.syslog.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SyslogMessage {
    private String facility;
    private String severity;
    private Instant timestamp;
    private String hostname;
    private String appName;
    private String procId;
    private String msgId;
    private String message;
    private Map<String, String> structuredData;
    private String protocol; // "RFC3164" or "RFC5424"
    private String sourceIp;
    private String rawMessage;

    // Constructors
    public SyslogMessage() {}

    public SyslogMessage(String facility, String severity, Instant timestamp, 
                        String hostname, String message, String protocol) {
        this.facility = facility;
        this.severity = severity;
        this.timestamp = timestamp;
        this.hostname = hostname;
        this.message = message;
        this.protocol = protocol;
    }

    // Getters and Setters
    public String getFacility() { return facility; }
    public void setFacility(String facility) { this.facility = facility; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getProcId() { return procId; }
    public void setProcId(String procId) { this.procId = procId; }

    public String getMsgId() { return msgId; }
    public void setMsgId(String msgId) { this.msgId = msgId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, String> getStructuredData() { return structuredData; }
    public void setStructuredData(Map<String, String> structuredData) { 
        this.structuredData = structuredData; 
    }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }

    public String getRawMessage() { return rawMessage; }
    public void setRawMessage(String rawMessage) { this.rawMessage = rawMessage; }
}
