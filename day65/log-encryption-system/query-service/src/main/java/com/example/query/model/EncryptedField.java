package com.example.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedField {
    private String fieldName;
    private String encryptedValue;
    private String keyId;
    private String iv;
    private String hmacHash;
}
