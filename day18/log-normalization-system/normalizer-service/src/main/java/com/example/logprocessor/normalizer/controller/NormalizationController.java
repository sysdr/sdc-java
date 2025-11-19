package com.example.logprocessor.normalizer.controller;

import com.example.logprocessor.common.format.LogFormat;
import com.example.logprocessor.common.model.*;
import com.example.logprocessor.normalizer.service.NormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/normalize")
@RequiredArgsConstructor
@Slf4j
public class NormalizationController {

    private final NormalizationService normalizationService;

    @PostMapping(consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<NormalizationResult> normalize(
            @RequestBody byte[] input,
            @RequestParam LogFormat targetFormat,
            @RequestHeader(value = "Content-Type", required = false) String contentType) {

        log.debug("Normalizing {} bytes to {}", input.length, targetFormat);
        NormalizationResult result = normalizationService.normalize(input, targetFormat, contentType);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/json")
    public ResponseEntity<NormalizationResult> normalizeJson(
            @RequestBody Map<String, Object> input,
            @RequestParam LogFormat targetFormat) {

        try {
            byte[] jsonBytes = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsBytes(input);
            NormalizationResult result = normalizationService.normalize(
                    jsonBytes, targetFormat, "application/json");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                NormalizationResult.failure("", LogFormat.JSON, targetFormat, e.getMessage())
            );
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchNormalizationResponse> normalizeBatch(
            @RequestBody BatchNormalizationRequest request) {

        log.debug("Processing batch of {} logs to {}", 
                request.getLogs().size(), request.getTargetFormat());
        BatchNormalizationResponse response = normalizationService.normalizeBatch(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/text")
    public ResponseEntity<NormalizationResult> normalizeText(
            @RequestBody String input,
            @RequestParam LogFormat targetFormat) {

        NormalizationResult result = normalizationService.normalize(
                input.getBytes(), targetFormat, "text/plain");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/base64")
    public ResponseEntity<Map<String, Object>> normalizeBase64(
            @RequestBody Map<String, String> request) {

        String data = request.get("data");
        String targetFormatStr = request.get("targetFormat");
        
        byte[] decoded = Base64.getDecoder().decode(data);
        LogFormat targetFormat = LogFormat.valueOf(targetFormatStr);
        
        NormalizationResult result = normalizationService.normalize(decoded, targetFormat);
        
        String encodedOutput = result.getData() != null 
                ? Base64.getEncoder().encodeToString(result.getData())
                : null;

        return ResponseEntity.ok(Map.of(
                "id", result.getId(),
                "success", result.isSuccess(),
                "sourceFormat", result.getSourceFormat().name(),
                "targetFormat", result.getTargetFormat().name(),
                "data", encodedOutput != null ? encodedOutput : "",
                "processingTimeMs", result.getProcessingTimeMs()
        ));
    }

    @GetMapping("/formats")
    public ResponseEntity<Set<LogFormat>> getSupportedFormats() {
        return ResponseEntity.ok(normalizationService.getSupportedFormats());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "normalizer"
        ));
    }
}
