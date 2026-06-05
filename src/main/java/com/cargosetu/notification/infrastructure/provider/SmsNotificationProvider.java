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
public class SmsNotificationProvider implements NotificationProvider {

    private final NotificationLogService logService;
    private final NotificationLogRepository repository;

    @Override
    @Retryable(
        retryFor = { NotificationDeliveryException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )
    public void send(NotificationEvent event, String recipient) {
        log.info("[SMS Thread: {}] Starting SMS delivery to: {}", Thread.currentThread().getName(), recipient);
        
        // Log the current attempt to DB in a new transaction
        logService.logAttempt(event.getEventId(), NotificationChannel.SMS, recipient);

        // Fetch current attempts from DB to simulate transient / permanent failures
        Optional<NotificationLog> logEntry = repository.findByEventIdAndChannelAndRecipient(
                event.getEventId(), NotificationChannel.SMS, recipient);
        
        int currentAttempt = logEntry.map(NotificationLog::getAttempts).orElse(1);
        log.info("SMS Send Attempt #{} for event: {}", currentAttempt, event.getEventName());

        // Failures simulation logic
        if (recipient.contains("permanent-fail")) {
            log.warn("Simulating permanent SMS failure on attempt #{}", currentAttempt);
            throw new NotificationDeliveryException("SMS gateway provider error (Permanent Simulated Failure)");
        } else if (recipient.contains("transient-fail") && currentAttempt < 3) {
            log.warn("Simulating transient SMS failure on attempt #{}", currentAttempt);
            throw new NotificationDeliveryException("SMS gateway network timeout (Transient Simulated Failure)");
        }

        // Simulate successful sending latency (e.g., calling Twilio)
        try {
            Thread.sleep(200); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NotificationDeliveryException("SMS sending thread interrupted", e);
        }

        log.info("SMS successfully sent to: {}", recipient);
        logService.logSuccess(event.getEventId(), NotificationChannel.SMS, recipient);
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.SMS;
    }

    /**
     * Fallback recovery method when all retries are exhausted.
     */
    @Recover
    public void recover(NotificationDeliveryException e, NotificationEvent event, String recipient) {
        log.error("[SMS Thread: {}] All SMS retry attempts exhausted for: {}. Failing gracefully. Reason: {}", 
                Thread.currentThread().getName(), recipient, e.getMessage());
        
        // Save the failure in DB
        logService.logFailure(event.getEventId(), NotificationChannel.SMS, recipient, e.getMessage());
    }
}
