package com.example.alerts.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RetryService {

    private final FailedNotificationRepository repository;
    private final NotificationConsumerService notificationService;

    @Scheduled(fixedDelay = 30000) // Check every 30 seconds
    public void retryFailedNotifications() {
        List<FailedNotification> pendingRetries = repository
            .findByStatusAndNextRetryAtBefore(
                FailedNotification.RetryStatus.PENDING,
                LocalDateTime.now()
            );

        for (FailedNotification notification : pendingRetries) {
            if (notification.getRetryCount() >= 5) {
                notification.setStatus(FailedNotification.RetryStatus.EXHAUSTED);
                repository.save(notification);
                log.warn("Retry exhausted for notification: {}", notification.getId());
                continue;
            }

            try {
                log.info("Retrying failed notification: {}", notification.getId());
                notification.setStatus(FailedNotification.RetryStatus.RETRYING);
                repository.save(notification);

                // Retry sending
                if ("pagerduty".equals(notification.getChannel())) {
                    notificationService.sendToPagerDuty(notification.getPayload());
                } else if ("slack".equals(notification.getChannel())) {
                    notificationService.sendToSlack(notification.getPayload());
                } else {
                    notificationService.sendToEmail(notification.getPayload());
                }

                notification.setStatus(FailedNotification.RetryStatus.SUCCEEDED);
                repository.save(notification);
                log.info("Retry succeeded for notification: {}", notification.getId());

            } catch (Exception e) {
                notification.setRetryCount(notification.getRetryCount() + 1);
                notification.setNextRetryAt(
                    LocalDateTime.now().plusMinutes((long) Math.pow(2, notification.getRetryCount()))
                );
                notification.setStatus(FailedNotification.RetryStatus.PENDING);
                repository.save(notification);
                log.warn("Retry failed for notification: {}", notification.getId());
            }
        }
    }
}
