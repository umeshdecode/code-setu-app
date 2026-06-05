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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.notification.twilio.from-number}")
    private String fromNumber;

    @Value("${app.notification.twilio.mock}")
    private boolean mockEnabled;

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

        // Fetch current attempts from DB to simulate transient / permanent failures (useful in mock & testing)
        Optional<NotificationLog> logEntry = repository.findByEventIdAndChannelAndRecipient(
                event.getEventId(), NotificationChannel.SMS, recipient);
        
        int currentAttempt = logEntry.map(NotificationLog::getAttempts).orElse(1);
        log.info("SMS Send Attempt #{} for event: {}", currentAttempt, event.getEventName());

        // Failures simulation logic for testing
        if (recipient.contains("permanent-fail")) {
            log.warn("Simulating permanent SMS failure on attempt #{}", currentAttempt);
            throw new NotificationDeliveryException("SMS gateway provider error (Permanent Simulated Failure)");
        } else if (recipient.contains("transient-fail") && currentAttempt < 3) {
            log.warn("Simulating transient SMS failure on attempt #{}", currentAttempt);
            throw new NotificationDeliveryException("SMS gateway network timeout (Transient Simulated Failure)");
        }

        if (mockEnabled) {
            log.info("[MOCK MODE] Simulating physical SMS sending to: {}. Sender: {}", recipient, fromNumber);
            try {
                Thread.sleep(200); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NotificationDeliveryException("SMS sending thread interrupted", e);
            }
        } else {
            try {
                log.info("[REAL MODE] Dispatching physical Twilio SMS to: {} from: {}", recipient, fromNumber);
                com.twilio.rest.api.v2010.account.Message.creator(
                        new com.twilio.type.PhoneNumber(recipient), // To
                        new com.twilio.type.PhoneNumber(fromNumber), // From
                        "CargoSetu Alert: Event " + event.getEventName() + " triggered (ID: " + event.getEventId() + ")"
                ).create();
            } catch (Exception e) {
                log.error("Failed to send Twilio SMS to: {}", recipient, e);
                throw new NotificationDeliveryException("Twilio Client Error: " + e.getMessage(), e);
            }
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
