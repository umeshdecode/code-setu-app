package com.cargosetu.notification.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker interface for all domain events that can trigger notifications.
 */
public interface NotificationEvent {
    UUID getEventId();
    Instant getTimestamp();
    String getEventName();
}
