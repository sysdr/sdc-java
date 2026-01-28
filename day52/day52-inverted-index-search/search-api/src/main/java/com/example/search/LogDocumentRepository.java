package com.example.search;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogDocumentRepository extends JpaRepository<LogDocument, Long> {
    
    @Query("SELECT d FROM LogDocument d WHERE d.id IN :ids ORDER BY d.timestamp DESC")
    List<LogDocument> findByIdIn(List<Long> ids);
}
