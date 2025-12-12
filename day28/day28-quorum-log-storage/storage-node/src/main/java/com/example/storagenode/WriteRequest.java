package com.example.storagenode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WriteRequest {
    private String key;
    private String value;
    private VersionVector version;
}
