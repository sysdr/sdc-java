package com.example.logprocessor.common.model;

import com.example.logprocessor.common.format.LogFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizationResult {
    private String id;
    private boolean success;
    private LogFormat sourceFormat;
    private LogFormat targetFormat;
    private byte[] data;
    private String errorMessage;
    private long processingTimeMs;

    public static NormalizationResult success(String id, LogFormat source, 
            LogFormat target, byte[] data, long timeMs) {
        return NormalizationResult.builder()
                .id(id)
                .success(true)
                .sourceFormat(source)
                .targetFormat(target)
                .data(data)
                .processingTimeMs(timeMs)
                .build();
    }

    public static NormalizationResult failure(String id, LogFormat source, 
            LogFormat target, String error) {
        return NormalizationResult.builder()
                .id(id)
                .success(false)
                .sourceFormat(source)
                .targetFormat(target)
                .errorMessage(error)
                .build();
    }
}
