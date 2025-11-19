package com.example.logprocessor.normalizer.detector;

import com.example.logprocessor.common.format.LogFormat;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FormatDetector {

    private final Counter detectionCounter;
    private final Counter fallbackCounter;

    public FormatDetector(MeterRegistry meterRegistry) {
        this.detectionCounter = meterRegistry.counter("format.detection.total");
        this.fallbackCounter = meterRegistry.counter("format.detection.fallback");
    }

    public LogFormat detect(byte[] input) {
        detectionCounter.increment();

        if (input == null || input.length == 0) {
            return LogFormat.TEXT;
        }

        // Check for Avro magic bytes: "Obj" followed by version byte
        if (input.length >= 4 && input[0] == 'O' && input[1] == 'b' 
                && input[2] == 'j' && input[3] == 1) {
            log.debug("Detected AVRO format via magic bytes");
            return LogFormat.AVRO;
        }

        // Check for JSON structure
        byte firstByte = skipWhitespace(input);
        if (firstByte == '{' || firstByte == '[') {
            if (isValidJson(input)) {
                log.debug("Detected JSON format");
                return LogFormat.JSON;
            }
        }

        // Check for Protobuf (heuristic: valid field tags)
        if (isLikelyProtobuf(input)) {
            log.debug("Detected PROTOBUF format via field tags");
            return LogFormat.PROTOBUF;
        }

        fallbackCounter.increment();
        log.debug("Falling back to TEXT format");
        return LogFormat.TEXT;
    }

    public LogFormat detect(byte[] input, String contentTypeHint) {
        if (contentTypeHint != null && !contentTypeHint.isEmpty()) {
            LogFormat hinted = LogFormat.fromContentType(contentTypeHint);
            if (hinted != LogFormat.TEXT || contentTypeHint.contains("text")) {
                log.debug("Using content-type hint: {}", hinted);
                return hinted;
            }
        }
        return detect(input);
    }

    private byte skipWhitespace(byte[] input) {
        for (byte b : input) {
            if (b != ' ' && b != '\t' && b != '\n' && b != '\r') {
                return b;
            }
        }
        return 0;
    }

    private boolean isValidJson(byte[] input) {
        int braces = 0;
        int brackets = 0;
        boolean inString = false;
        boolean escaped = false;

        for (byte b : input) {
            if (escaped) {
                escaped = false;
                continue;
            }
            if (b == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (b == '"') {
                inString = !inString;
                continue;
            }
            if (inString) continue;

            switch (b) {
                case '{': braces++; break;
                case '}': braces--; break;
                case '[': brackets++; break;
                case ']': brackets--; break;
            }

            if (braces < 0 || brackets < 0) return false;
        }

        return braces == 0 && brackets == 0;
    }

    private boolean isLikelyProtobuf(byte[] input) {
        if (input.length < 2) return false;

        // Protobuf field tags have specific patterns
        // Field number (1-15) with wire type (0-5)
        int firstTag = input[0] & 0xFF;
        int wireType = firstTag & 0x07;
        int fieldNumber = firstTag >> 3;

        // Valid wire types: 0 (varint), 1 (64-bit), 2 (length-delimited), 5 (32-bit)
        boolean validWireType = wireType == 0 || wireType == 1 || 
                                wireType == 2 || wireType == 5;
        boolean validFieldNumber = fieldNumber >= 1 && fieldNumber <= 536870911;

        return validWireType && validFieldNumber;
    }
}
