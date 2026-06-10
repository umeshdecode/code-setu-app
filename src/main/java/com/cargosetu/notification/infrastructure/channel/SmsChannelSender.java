package com.cargosetu.notification.infrastructure.channel;

import com.cargosetu.notification.exception.NotificationDeliveryException;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationStatus;
import com.cargosetu.notification.model.NotificationAudit;
import com.cargosetu.notification.repository.NotificationAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsChannelSender implements NotificationChannelSender {

    private final NotificationAuditRepository auditRepository;

    @Value("${app.notification.twilio.from-number}")
    private String fromNumber;

    @Value("${app.notification.twilio.mock}")
    private boolean mockEnabled;

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.SMS;
    }

    @Override
    @Retryable(
        retryFor = { Exception.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )
    public void send(NotificationAudit audit) throws Exception {
        String recipient = audit.getRecipient();
        
        // Log the current attempt to DB
        audit.setRetryCount(audit.getRetryCount() + 1);
        audit.setStatus(audit.getRetryCount() <= 1 ? NotificationStatus.PROCESSING : NotificationStatus.RETRYING);
        auditRepository.save(audit);

        int currentAttempt = audit.getRetryCount();
        log.info("[SMS Channel] Sending SMS to: {} (Attempt #{})", recipient, currentAttempt);

        // Failures simulation logic for testing
        if (recipient.contains("permanent-fail")) {
            log.warn("Simulating permanent SMS failure on attempt #{}", currentAttempt);
            throw new NotificationDeliveryException("SMS gateway provider error (Permanent Simulated Failure)");
        } else if (recipient.contains("transient-fail") && currentAttempt < 3) {
            log.warn("Simulating transient SMS failure on attempt #{}", currentAttempt);
            throw new NotificationDeliveryException("SMS gateway network timeout (Transient Simulated Failure)");
        }

        if (mockEnabled) {
            log.info("[MOCK SMS] Dispatching SMS to: {}. Sender: {}. Message: '{}'", recipient, fromNumber, audit.getMessageBody());
            Thread.sleep(200); // Simulate network latency
        } else {
            try {
                log.info("[REAL SMS] Dispatching physical Twilio SMS to: {} from: {}", recipient, fromNumber);
                com.twilio.rest.api.v2010.account.Message.creator(
                        new com.twilio.type.PhoneNumber(recipient), // To
                        new com.twilio.type.PhoneNumber(fromNumber), // From
                        audit.getMessageBody()
                ).create();
            } catch (Exception e) {
                log.error("Failed to send Twilio SMS to: {}", recipient, e);
                throw new NotificationDeliveryException("Twilio Client Error: " + e.getMessage(), e);
            }
        }

        // Success!
        audit.setStatus(NotificationStatus.SENT);
        audit.setSentAt(LocalDateTime.now());
        audit.setDeliveredAt(LocalDateTime.now());
        auditRepository.save(audit);
        log.info("[SMS Channel] SMS sent successfully to: {}", recipient);
    }

    @Recover
    public void recover(Exception e, NotificationAudit audit) {
        log.error("[SMS Channel] All SMS retry attempts exhausted for: {}. Failing gracefully. Reason: {}", 
                audit.getRecipient(), e.getMessage());
        audit.setStatus(NotificationStatus.FAILED);
        audit.setFailureReason(e.getMessage());
        auditRepository.save(audit);
    }
}
