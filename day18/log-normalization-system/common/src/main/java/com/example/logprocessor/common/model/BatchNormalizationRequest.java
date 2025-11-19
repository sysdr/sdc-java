package com.example.logprocessor.common.model;

import com.example.logprocessor.common.format.LogFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchNormalizationRequest {
    private List<LogEntry> logs;
    private LogFormat targetFormat;
    private boolean preserveRaw;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogEntry {
        private String id;
        private byte[] data;
        private LogFormat format;
    }
}
