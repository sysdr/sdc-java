package com.example.logprocessor.normalizer.handler;

import com.example.logprocessor.common.format.LogFormat;
import com.example.logprocessor.common.model.CanonicalLog;

public interface FormatHandler {
    LogFormat getFormat();
    CanonicalLog parse(byte[] input) throws FormatParseException;
    byte[] serialize(CanonicalLog log) throws FormatSerializeException;

    class FormatParseException extends Exception {
        public FormatParseException(String message) {
            super(message);
        }
        public FormatParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    class FormatSerializeException extends Exception {
        public FormatSerializeException(String message) {
            super(message);
        }
        public FormatSerializeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
