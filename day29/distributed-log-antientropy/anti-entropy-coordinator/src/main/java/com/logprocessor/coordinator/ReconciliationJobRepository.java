package com.logprocessor.coordinator;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ReconciliationJobRepository extends JpaRepository<ReconciliationJob, Long> {
    List<ReconciliationJob> findByStatusOrderByPriorityDesc(String status);
    
    @Query("SELECT j FROM ReconciliationJob j WHERE j.status = 'PENDING' ORDER BY j.priority DESC, j.scheduledAt ASC")
    List<ReconciliationJob> findPendingJobsByPriority();
    
    List<ReconciliationJob> findByPartitionIdAndStatus(String partitionId, String status);
}
