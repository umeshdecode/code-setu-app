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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    private final JavaMailSender mailSender;

    @Value("${app.notification.email.from}")
    private String fromEmail;

    @Value("${app.notification.email.mock}")
    private boolean mockEnabled;

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

        // Fetch current attempts from DB to simulate transient / permanent failures (useful in mock & testing)
        Optional<NotificationLog> logEntry = repository.findByEventIdAndChannelAndRecipient(
                event.getEventId(), NotificationChannel.EMAIL, recipient);
        
        int currentAttempt = logEntry.map(NotificationLog::getAttempts).orElse(1);
        log.info("Email Send Attempt #{} for event: {}", currentAttempt, event.getEventName());

        // Failures simulation logic for testing
        if (recipient.contains("permanent-fail")) {
            log.warn("Simulating permanent email failure on attempt #{}", currentAttempt);
            throw new NotificationDeliveryException("SMTP server connection timeout (Permanent Simulated Failure)");
        } else if (recipient.contains("transient-fail") && currentAttempt < 3) {
            log.warn("Simulating transient email failure on attempt #{}", currentAttempt);
            throw new NotificationDeliveryException("Temporary SMTP server throttling (Transient Simulated Failure)");
        }

        if (mockEnabled) {
            log.info("[MOCK MODE] Simulating physical email sending to: {}. Sender: {}", recipient, fromEmail);
            try {
                Thread.sleep(300); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NotificationDeliveryException("Email sending thread interrupted", e);
            }
        } else {
            try {
                log.info("[REAL MODE] Dispatching physical SMTP email to: {} using JavaMailSender", recipient);
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(recipient);
                message.setSubject("CargoSetu notification: " + event.getEventName());
                message.setText("Dear Customer,\n\nThis is a real-time notification from CargoSetu App.\n\n" +
                        "Event: " + event.getEventName() + "\n" +
                        "Event ID: " + event.getEventId() + "\n" +
                        "Timestamp: " + event.getTimestamp() + "\n\n" +
                        "Best regards,\nPackers & Mover ERP Management System");
                
                mailSender.send(message);
            } catch (Exception e) {
                log.error("Failed to send SMTP email to: {}", recipient, e);
                throw new NotificationDeliveryException("SMTP client error: " + e.getMessage(), e);
            }
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
