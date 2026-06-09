package com.cargosetu.notification.service;

import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationStatus;
import com.cargosetu.notification.model.EmailNotificationAudit;
import com.cargosetu.notification.model.SmsNotificationAudit;
import com.cargosetu.notification.model.PushNotificationAudit;
import com.cargosetu.notification.repository.EmailNotificationAuditRepository;
import com.cargosetu.notification.repository.SmsNotificationAuditRepository;
import com.cargosetu.notification.repository.PushNotificationAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationLogServiceImpl implements NotificationLogService {

    private final EmailNotificationAuditRepository emailRepository;
    private final SmsNotificationAuditRepository smsRepository;
    private final PushNotificationAuditRepository pushRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createPendingLog(UUID eventId, String eventName, NotificationChannel channel, String recipient) {
        log.debug("Creating pending notification audit: eventId={}, channel={}, recipient={}", eventId, channel, recipient);
        String refId = eventId.toString();

        if (channel == NotificationChannel.EMAIL) {
            EmailNotificationAudit audit = new EmailNotificationAudit();
            audit.setReferenceId(refId);
            audit.setEventCode(eventName);
            audit.setRecipientEmail(recipient);
            audit.setStatus(NotificationStatus.PENDING);
            audit.setRetryCount(0);
            emailRepository.save(audit);
        } else if (channel == NotificationChannel.SMS) {
            SmsNotificationAudit audit = new SmsNotificationAudit();
            audit.setReferenceId(refId);
            audit.setEventCode(eventName);
            audit.setMobileNumber(recipient);
            audit.setStatus(NotificationStatus.PENDING);
            audit.setRetryCount(0);
            smsRepository.save(audit);
        } else if (channel == NotificationChannel.PUSH) {
            PushNotificationAudit audit = new PushNotificationAudit();
            audit.setReferenceId(refId);
            audit.setEventCode(eventName);
            audit.setPushToken(recipient);
            audit.setStatus(NotificationStatus.PENDING);
            audit.setRetryCount(0);
            pushRepository.save(audit);
        } else {
            log.warn("Unsupported channel {} for pending audit logging", channel);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAttempt(UUID eventId, NotificationChannel channel, String recipient) {
        log.debug("Logging notification attempt: eventId={}, channel={}, recipient={}", eventId, channel, recipient);
        String refId = eventId.toString();

        if (channel == NotificationChannel.EMAIL) {
            emailRepository.findByReferenceIdAndRecipientEmail(refId, recipient)
                    .ifPresentOrElse(audit -> {
                        audit.setRetryCount(audit.getRetryCount() + 1);
                        audit.setStatus(audit.getRetryCount() <= 1 ? NotificationStatus.PENDING : NotificationStatus.RETRYING);
                        emailRepository.save(audit);
                    }, () -> log.warn("Email audit entry not found to update attempt for eventId={}, recipient={}", eventId, recipient));
        } else if (channel == NotificationChannel.SMS) {
            smsRepository.findByReferenceIdAndMobileNumber(refId, recipient)
                    .ifPresentOrElse(audit -> {
                        audit.setRetryCount(audit.getRetryCount() + 1);
                        audit.setStatus(audit.getRetryCount() <= 1 ? NotificationStatus.PENDING : NotificationStatus.RETRYING);
                        smsRepository.save(audit);
                    }, () -> log.warn("SMS audit entry not found to update attempt for eventId={}, recipient={}", eventId, recipient));
        } else if (channel == NotificationChannel.PUSH) {
            pushRepository.findByReferenceIdAndPushToken(refId, recipient)
                    .ifPresentOrElse(audit -> {
                        audit.setRetryCount(audit.getRetryCount() + 1);
                        audit.setStatus(audit.getRetryCount() <= 1 ? NotificationStatus.PENDING : NotificationStatus.RETRYING);
                        pushRepository.save(audit);
                    }, () -> log.warn("Push audit entry not found to update attempt for eventId={}, recipient={}", eventId, recipient));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(UUID eventId, NotificationChannel channel, String recipient) {
        log.info("Notification sent successfully: eventId={}, channel={}, recipient={}", eventId, channel, recipient);
        String refId = eventId.toString();

        if (channel == NotificationChannel.EMAIL) {
            emailRepository.findByReferenceIdAndRecipientEmail(refId, recipient)
                    .ifPresentOrElse(audit -> {
                        audit.setStatus(NotificationStatus.SENT);
                        audit.setFailureReason(null);
                        audit.setSentAt(LocalDateTime.now());
                        audit.setDeliveredAt(LocalDateTime.now());
                        emailRepository.save(audit);
                    }, () -> log.warn("Email audit entry not found to update success for eventId={}, recipient={}", eventId, recipient));
        } else if (channel == NotificationChannel.SMS) {
            smsRepository.findByReferenceIdAndMobileNumber(refId, recipient)
                    .ifPresentOrElse(audit -> {
                        audit.setStatus(NotificationStatus.SENT);
                        audit.setFailureReason(null);
                        audit.setSentAt(LocalDateTime.now());
                        audit.setDeliveredAt(LocalDateTime.now());
                        smsRepository.save(audit);
                    }, () -> log.warn("SMS audit entry not found to update success for eventId={}, recipient={}", eventId, recipient));
        } else if (channel == NotificationChannel.PUSH) {
            pushRepository.findByReferenceIdAndPushToken(refId, recipient)
                    .ifPresentOrElse(audit -> {
                        audit.setStatus(NotificationStatus.SENT);
                        audit.setFailureReason(null);
                        audit.setSentAt(LocalDateTime.now());
                        audit.setDeliveredAt(LocalDateTime.now());
                        pushRepository.save(audit);
                    }, () -> log.warn("Push audit entry not found to update success for eventId={}, recipient={}", eventId, recipient));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(UUID eventId, NotificationChannel channel, String recipient, String errorMessage) {
        log.error("Notification delivery failed: eventId={}, channel={}, recipient={}, error={}", 
                eventId, channel, recipient, errorMessage);
        String refId = eventId.toString();

        if (channel == NotificationChannel.EMAIL) {
            emailRepository.findByReferenceIdAndRecipientEmail(refId, recipient)
                    .ifPresentOrElse(audit -> {
                        audit.setStatus(NotificationStatus.FAILED);
                        audit.setFailureReason(errorMessage);
                        emailRepository.save(audit);
                    }, () -> log.warn("Email audit entry not found to update failure for eventId={}, recipient={}", eventId, recipient));
        } else if (channel == NotificationChannel.SMS) {
            smsRepository.findByReferenceIdAndMobileNumber(refId, recipient)
                    .ifPresentOrElse(audit -> {
                        audit.setStatus(NotificationStatus.FAILED);
                        audit.setFailureReason(errorMessage);
                        smsRepository.save(audit);
                    }, () -> log.warn("SMS audit entry not found to update failure for eventId={}, recipient={}", eventId, recipient));
        } else if (channel == NotificationChannel.PUSH) {
            pushRepository.findByReferenceIdAndPushToken(refId, recipient)
                    .ifPresentOrElse(audit -> {
                        audit.setStatus(NotificationStatus.FAILED);
                        audit.setFailureReason(errorMessage);
                        pushRepository.save(audit);
                    }, () -> log.warn("Push audit entry not found to update failure for eventId={}, recipient={}", eventId, recipient));
        }
    }
}
