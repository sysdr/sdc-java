package com.example.indexing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoredDocument {
    private Long docId;
    private Double score;
}
