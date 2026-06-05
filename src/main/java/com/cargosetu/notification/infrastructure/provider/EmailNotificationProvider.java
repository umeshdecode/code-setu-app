package com.cargosetu.notification.infrastructure.provider;

import com.cargosetu.notification.event.NotificationEvent;
import com.cargosetu.notification.exception.NotificationDeliveryException;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationLog;
import com.cargosetu.notification.repository.NotificationLogRepository;
import com.cargosetu.notification.service.NotificationLogService;
import com.cargosetu.notification.service.NotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationProvider implements NotificationProvider {

    private final NotificationLogService logService;
    private final NotificationLogRepository repository;

    @Override
    @Retryable(
        retryFor = { NotificationDeliveryException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )
    public void send(NotificationEvent event, String recipient) {
        log.info("[Email Thread: {}] Starting email delivery to: {}", Thread.currentThread().getName(), recipient);
        
        // Log the current attempt to DB in a new transaction
        logService.logAttempt(event.getEventId(), NotificationChannel.EMAIL, recipient);

        // Fetch current attempts from DB to simulate transient / permanent failures
        Optional<NotificationLog> logEntry = repository.findByEventIdAndChannelAndRecipient(
                event.getEventId(), NotificationChannel.EMAIL, recipient);
        
        int currentAttempt = logEntry.map(NotificationLog::getAttempts).orElse(1);
        log.info("Email Send Attempt #{} for event: {}", currentAttempt, event.getEventName());

        // Failures simulation logic
        if (recipient.contains("permanent-fail")) {
            log.warn("Simulating permanent email failure on attempt #{}", currentAttempt);
            throw new NotificationDeliveryException("SMTP server connection timeout (Permanent Simulated Failure)");
        } else if (recipient.contains("transient-fail") && currentAttempt < 3) {
            log.warn("Simulating transient email failure on attempt #{}", currentAttempt);
            throw new NotificationDeliveryException("Temporary SMTP server throttling (Transient Simulated Failure)");
        }

        // Simulate successful sending latency (e.g., calling SendGrid/SES)
        try {
            Thread.sleep(300); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NotificationDeliveryException("Email sending thread interrupted", e);
        }

        log.info("Email successfully sent to: {}", recipient);
        logService.logSuccess(event.getEventId(), NotificationChannel.EMAIL, recipient);
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    /**
     * Fallback recovery method when all retries are exhausted.
     */
    @Recover
    public void recover(NotificationDeliveryException e, NotificationEvent event, String recipient) {
        log.error("[Email Thread: {}] All email retry attempts exhausted for: {}. Failing gracefully. Reason: {}", 
                Thread.currentThread().getName(), recipient, e.getMessage());
        
        // Save the failure in DB
        logService.logFailure(event.getEventId(), NotificationChannel.EMAIL, recipient, e.getMessage());
    }
}
