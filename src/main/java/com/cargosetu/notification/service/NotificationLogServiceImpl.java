package com.cargosetu.notification.service;

import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationLog;
import com.cargosetu.notification.model.NotificationStatus;
import com.cargosetu.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationLogServiceImpl implements NotificationLogService {

    private final NotificationLogRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createPendingLog(UUID eventId, String eventName, NotificationChannel channel, String recipient) {
        log.debug("Creating pending notification log: eventId={}, channel={}, recipient={}", eventId, channel, recipient);
        NotificationLog logEntry = NotificationLog.builder()
                .eventId(eventId)
                .eventName(eventName)
                .channel(channel)
                .recipient(recipient)
                .status(NotificationStatus.PENDING)
                .attempts(0)
                .build();
        repository.save(logEntry);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAttempt(UUID eventId, NotificationChannel channel, String recipient) {
        log.debug("Logging notification attempt: eventId={}, channel={}, recipient={}", eventId, channel, recipient);
        repository.findByEventIdAndChannelAndRecipient(eventId, channel, recipient)
                .ifPresentOrElse(logEntry -> {
                    logEntry.setAttempts(logEntry.getAttempts() + 1);
                    logEntry.setStatus(logEntry.getAttempts() == 1 ? NotificationStatus.PENDING : NotificationStatus.RETRYING);
                    repository.save(logEntry);
                }, () -> log.warn("Log entry not found to update attempt for eventId={}, channel={}", eventId, channel));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(UUID eventId, NotificationChannel channel, String recipient) {
        log.info("Notification sent successfully: eventId={}, channel={}, recipient={}", eventId, channel, recipient);
        repository.findByEventIdAndChannelAndRecipient(eventId, channel, recipient)
                .ifPresentOrElse(logEntry -> {
                    logEntry.setStatus(NotificationStatus.SENT);
                    logEntry.setLastErrorMessage(null);
                    repository.save(logEntry);
                }, () -> log.warn("Log entry not found to update success for eventId={}, channel={}", eventId, channel));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(UUID eventId, NotificationChannel channel, String recipient, String errorMessage) {
        log.error("Notification delivery failed: eventId={}, channel={}, recipient={}, error={}", 
                eventId, channel, recipient, errorMessage);
        repository.findByEventIdAndChannelAndRecipient(eventId, channel, recipient)
                .ifPresentOrElse(logEntry -> {
                    logEntry.setStatus(NotificationStatus.FAILED);
                    logEntry.setLastErrorMessage(errorMessage);
                    repository.save(logEntry);
                }, () -> log.warn("Log entry not found to update failure for eventId={}, channel={}", eventId, channel));
    }
}
