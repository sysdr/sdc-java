package com.example.logprocessor.producer;

public enum CompressionAlgorithm {
    NONE("none"),
    GZIP("gzip"),
    SNAPPY("snappy"),
    LZ4("lz4");

    private final String name;

    CompressionAlgorithm(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
