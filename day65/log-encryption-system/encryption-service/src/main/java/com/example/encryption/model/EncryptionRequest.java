package com.example.encryption.model;

import lombok.Data;
import java.util.Map;

@Data
public class EncryptionRequest {
    private Map<String, String> fields; // fieldName -> plaintext value
    private String classification; // PII, INTERNAL, PUBLIC
}
