package com.example.logconsumer.repository;

import com.example.logconsumer.model.LogEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEventRepository extends JpaRepository<LogEvent, String> {
}
