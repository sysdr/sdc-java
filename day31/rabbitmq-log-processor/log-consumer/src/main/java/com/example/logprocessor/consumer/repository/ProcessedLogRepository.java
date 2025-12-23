package com.example.logprocessor.consumer.repository;

import com.example.logprocessor.consumer.model.ProcessedLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedLogRepository extends JpaRepository<ProcessedLog, String> {
}
