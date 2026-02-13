package com.example.query.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    private String userId;
    private String role; // ADMIN, SUPPORT, ANALYST, COMPLIANCE
    private String email;
}
