package com.example.encryption.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EncryptionKey implements Serializable {
    private String keyId;
    private String algorithm;
    private byte[] key;
    private Instant validFrom;
    private Instant validUntil;
    private int version;
}
