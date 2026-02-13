package com.example.encryption.controller;

import com.example.encryption.model.EncryptedField;
import com.example.encryption.model.EncryptionRequest;
import com.example.encryption.model.EncryptionResponse;
import com.example.encryption.service.FieldEncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/encryption")
@RequiredArgsConstructor
public class EncryptionController {
    
    private final FieldEncryptionService encryptionService;
    
    @PostMapping("/encrypt")
    public EncryptionResponse encryptFields(@RequestBody EncryptionRequest request) {
        long startTime = System.currentTimeMillis();
        
        List<EncryptedField> encryptedFields = request.getFields().entrySet().stream()
            .map(entry -> encryptionService.encrypt(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        return new EncryptionResponse(encryptedFields, processingTime);
    }
    
    @PostMapping("/decrypt")
    public String decryptField(@RequestBody EncryptedField encryptedField) {
        return encryptionService.decrypt(encryptedField);
    }
}
