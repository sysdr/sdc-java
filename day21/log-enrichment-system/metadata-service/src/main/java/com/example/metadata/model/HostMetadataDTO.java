package com.example.metadata.model;

import lombok.Data;

@Data
public class HostMetadataDTO {
    private String hostname;
    private String ipAddress;
    private String datacenter;
    private String environment;
    private String costCenter;
}
