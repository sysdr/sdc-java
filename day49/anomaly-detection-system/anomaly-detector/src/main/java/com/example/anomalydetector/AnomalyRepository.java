package com.example.anomalydetector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnomalyRepository extends JpaRepository<AnomalyEntity, Long> {
    
    List<AnomalyEntity> findByServiceNameOrderByTimestampDesc(String serviceName);
    
    List<AnomalyEntity> findByTimestampBetweenOrderByConfidenceDesc(Long startTime, Long endTime);
    
    @Query("SELECT a FROM AnomalyEntity a WHERE a.confidence > :minConfidence ORDER BY a.timestamp DESC")
    List<AnomalyEntity> findHighConfidenceAnomalies(@Param("minConfidence") Double minConfidence);
}
