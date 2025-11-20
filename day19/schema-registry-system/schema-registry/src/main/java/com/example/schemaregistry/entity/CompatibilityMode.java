package com.example.schemaregistry.entity;

public enum CompatibilityMode {
    BACKWARD,      // New schema can read old data
    FORWARD,       // Old schema can read new data
    FULL,          // Both backward and forward
    NONE           // No compatibility checking
}
