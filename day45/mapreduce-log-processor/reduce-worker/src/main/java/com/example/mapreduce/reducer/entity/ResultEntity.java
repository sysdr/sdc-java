package com.example.mapreduce.reducer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "results", indexes = {
    @Index(name = "idx_job_key", columnList = "jobId,resultKey")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String jobId;
    private String resultKey;
    private Long resultValue;
}
