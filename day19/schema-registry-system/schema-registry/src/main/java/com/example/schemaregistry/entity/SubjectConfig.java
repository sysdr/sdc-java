package com.example.schemaregistry.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "subject_config")
public class SubjectConfig {
    
    @Id
    @Column(length = 255)
    private String subject;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "compatibility_mode", nullable = false)
    private CompatibilityMode compatibilityMode = CompatibilityMode.BACKWARD;
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public CompatibilityMode getCompatibilityMode() { return compatibilityMode; }
    public void setCompatibilityMode(CompatibilityMode compatibilityMode) { 
        this.compatibilityMode = compatibilityMode; 
    }
}
