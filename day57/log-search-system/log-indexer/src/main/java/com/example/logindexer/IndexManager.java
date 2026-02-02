package com.example.logindexer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class IndexManager {
    private static final Logger logger = LoggerFactory.getLogger(IndexManager.class);
    private static final String INDEX_NAME = "logs";
    
    private final ElasticsearchClient esClient;
    
    public IndexManager(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }
    
    @PostConstruct
    public void initialize() {
        try {
            if (!indexExists()) {
                createIndex();
                logger.info("Created Elasticsearch index: {}", INDEX_NAME);
            } else {
                logger.info("Elasticsearch index already exists: {}", INDEX_NAME);
            }
        } catch (IOException e) {
            logger.error("Failed to initialize Elasticsearch index", e);
            throw new RuntimeException("Failed to initialize Elasticsearch", e);
        }
    }
    
    private boolean indexExists() throws IOException {
        return esClient.indices().exists(
            ExistsRequest.of(e -> e.index(INDEX_NAME))
        ).value();
    }
    
    private void createIndex() throws IOException {
        esClient.indices().create(CreateIndexRequest.of(c -> c
            .index(INDEX_NAME)
            .settings(IndexSettings.of(s -> s
                .numberOfShards("5")
                .numberOfReplicas("1")
                .refreshInterval(t -> t.time("1s"))
            ))
            .mappings(TypeMapping.of(m -> m
                .properties("id", Property.of(p -> p
                    .keyword(KeywordProperty.of(k -> k))))
                .properties("timestamp", Property.of(p -> p
                    .date(DateProperty.of(d -> d))))
                .properties("severity", Property.of(p -> p
                    .keyword(KeywordProperty.of(k -> k))))
                .properties("service_name", Property.of(p -> p
                    .keyword(KeywordProperty.of(k -> k))))
                .properties("message", Property.of(p -> p
                    .text(TextProperty.of(t -> t
                        .analyzer("standard")
                        .fields("keyword", Property.of(f -> f
                            .keyword(KeywordProperty.of(k -> k))))))))
                .properties("stack_trace", Property.of(p -> p
                    .text(TextProperty.of(t -> t))))
                .properties("metadata", Property.of(p -> p
                    .object(ObjectProperty.of(o -> o))))
                .properties("trace_id", Property.of(p -> p
                    .keyword(KeywordProperty.of(k -> k))))
                .properties("span_id", Property.of(p -> p
                    .keyword(KeywordProperty.of(k -> k))))
            ))
        ));
    }
}
