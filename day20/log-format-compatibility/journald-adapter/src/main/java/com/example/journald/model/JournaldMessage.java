package com.example.journald.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JournaldMessage {
    private Instant timestamp;
    private String priority;
    private String hostname;
    private String unit;
    private String message;
    private String pid;
    private String uid;
    private String gid;
    private String comm;
    private String exe;
    private String cmdline;
    private String cgroup;
    private String containerId;
    private Map<String, String> additionalFields;

    // Constructors
    public JournaldMessage() {}

    // Getters and Setters
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPid() { return pid; }
    public void setPid(String pid) { this.pid = pid; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getGid() { return gid; }
    public void setGid(String gid) { this.gid = gid; }

    public String getComm() { return comm; }
    public void setComm(String comm) { this.comm = comm; }

    public String getExe() { return exe; }
    public void setExe(String exe) { this.exe = exe; }

    public String getCmdline() { return cmdline; }
    public void setCmdline(String cmdline) { this.cmdline = cmdline; }

    public String getCgroup() { return cgroup; }
    public void setCgroup(String cgroup) { this.cgroup = cgroup; }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }

    public Map<String, String> getAdditionalFields() { return additionalFields; }
    public void setAdditionalFields(Map<String, String> additionalFields) { 
        this.additionalFields = additionalFields; 
    }
}
