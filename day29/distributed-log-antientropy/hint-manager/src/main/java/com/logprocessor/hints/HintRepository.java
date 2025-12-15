package com.logprocessor.hints;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;

public interface HintRepository extends JpaRepository<Hint, Long> {
    List<Hint> findByStatusOrderByCreatedAt(String status);
    
    List<Hint> findByTargetNodeUrlAndStatus(String targetNodeUrl, String status);
    
    @Query("SELECT h FROM Hint h WHERE h.status = 'PENDING' AND h.createdAt < :expiryTime")
    List<Hint> findExpiredHints(Instant expiryTime);
    
    Long countByTargetNodeUrlAndStatus(String targetNodeUrl, String status);
}
