package com.example.encryption.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedField implements Serializable {
    private String fieldName;
    private String encryptedValue;
    private String keyId;
    private String iv; // Initialization Vector for GCM
    private String hmacHash; // For deterministic searchable encryption
}
