package com.example.facetedsearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "logs")
public class LogDocument {
    @Id
    private String id;
    
    @Field(type = FieldType.Date)
    private Instant timestamp;
    
    @Field(type = FieldType.Keyword)
    private String level;
    
    @Field(type = FieldType.Keyword)
    private String service;
    
    @Field(type = FieldType.Keyword)
    private String environment;
    
    @Field(type = FieldType.Keyword)
    private String host;
    
    @Field(type = FieldType.Keyword)
    private String region;
    
    @Field(type = FieldType.Integer)
    private Integer statusCode;
    
    @Field(type = FieldType.Keyword)
    private String errorType;
    
    @Field(type = FieldType.Text)
    private String message;
    
    @Field(type = FieldType.Long)
    private Long durationMs;
    
    @Field(type = FieldType.Keyword)
    private String userId;
    
    @Field(type = FieldType.Keyword)
    private String traceId;
}
