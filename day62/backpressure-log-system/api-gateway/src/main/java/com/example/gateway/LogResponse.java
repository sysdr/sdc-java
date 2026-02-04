package com.example.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LogResponse {
    private String message;
    private String metadata;
}
