package com.example.encryption.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class EncryptionResponse {
    private List<EncryptedField> encryptedFields;
    private long processingTimeMs;
}
