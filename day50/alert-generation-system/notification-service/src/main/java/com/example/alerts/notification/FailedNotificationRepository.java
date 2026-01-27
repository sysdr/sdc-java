package com.example.alerts.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FailedNotificationRepository extends JpaRepository<FailedNotification, String> {
    
    List<FailedNotification> findByStatusAndNextRetryAtBefore(
        FailedNotification.RetryStatus status,
        LocalDateTime now
    );
}
