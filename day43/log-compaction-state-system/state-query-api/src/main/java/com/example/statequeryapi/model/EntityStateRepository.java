package com.example.statequeryapi.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntityStateRepository extends JpaRepository<EntityState, String> {
    List<EntityState> findByEntityType(String entityType);
    List<EntityState> findByStatus(String status);
    
    @Query("SELECT COUNT(e) FROM EntityState e WHERE e.entityType = :entityType")
    long countByEntityType(String entityType);
}
