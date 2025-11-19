package com.example.logprocessor.common.format;

public enum LogFormat {
    TEXT("text/plain"),
    JSON("application/json"),
    PROTOBUF("application/x-protobuf"),
    AVRO("application/avro");

    private final String contentType;

    LogFormat(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public static LogFormat fromContentType(String contentType) {
        for (LogFormat format : values()) {
            if (format.contentType.equalsIgnoreCase(contentType)) {
                return format;
            }
        }
        return TEXT;
    }
}
