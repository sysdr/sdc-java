package com.example.logprocessor.producer;

import java.time.Instant;
import java.util.Map;

public record LogEvent(
    String id,
    String level,
    String message,
    String source,
    Instant timestamp,
    Map<String, Object> metadata
) {}
