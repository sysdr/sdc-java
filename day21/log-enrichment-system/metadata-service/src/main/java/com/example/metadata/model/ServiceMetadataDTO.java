package com.example.metadata.model;

import lombok.Data;

@Data
public class ServiceMetadataDTO {
    private String serviceName;
    private String version;
    private String deploymentId;
    private String team;
    private String costCenter;
}
