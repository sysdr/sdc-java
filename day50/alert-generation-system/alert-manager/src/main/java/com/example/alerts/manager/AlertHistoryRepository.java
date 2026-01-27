package com.example.alerts.manager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, String> {
    
    Optional<AlertHistory> findByAlertId(String alertId);
    
    List<AlertHistory> findByStateAndTriggeredAtBefore(
        AlertHistory.AlertState state, 
        LocalDateTime threshold
    );
    
    @Query("SELECT COUNT(a) FROM AlertHistory a WHERE a.state = ?1 AND a.triggeredAt > ?2")
    Long countByStateAfter(AlertHistory.AlertState state, LocalDateTime after);
}
