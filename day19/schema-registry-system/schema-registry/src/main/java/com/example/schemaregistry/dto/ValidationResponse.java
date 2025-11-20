package com.example.schemaregistry.dto;

import java.util.ArrayList;
import java.util.List;

public class ValidationResponse {
    private boolean valid;
    private List<String> errors = new ArrayList<>();
    
    public ValidationResponse() {}
    
    public ValidationResponse(boolean valid) {
        this.valid = valid;
    }
    
    public static ValidationResponse valid() {
        return new ValidationResponse(true);
    }
    
    public static ValidationResponse invalid(List<String> errors) {
        ValidationResponse response = new ValidationResponse(false);
        response.setErrors(errors);
        return response;
    }
    
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}
