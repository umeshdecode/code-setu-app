package com.cargosetu.notification.service;

import com.cargosetu.notification.model.NotificationChannel;
import java.util.UUID;

public interface NotificationLogService {
    
    void createPendingLog(UUID eventId, String eventName, NotificationChannel channel, String recipient);
    
    void logAttempt(UUID eventId, NotificationChannel channel, String recipient);
    
    void logSuccess(UUID eventId, NotificationChannel channel, String recipient);
    
    void logFailure(UUID eventId, NotificationChannel channel, String recipient, String errorMessage);
}
