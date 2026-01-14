package com.example.stateconsumer.repository;

import com.example.stateconsumer.model.EntityState;
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
    
    @Query("SELECT e.entityType, COUNT(e) FROM EntityState e GROUP BY e.entityType")
    List<Object[]> countByEntityTypeGrouped();
}
