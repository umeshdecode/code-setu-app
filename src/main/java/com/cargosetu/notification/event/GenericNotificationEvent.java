package com.cargosetu.notification.event;

import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationEventType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GenericNotificationEvent(
    UUID eventId,
    NotificationEventType templateCode,
    List<String> recipients,
    NotificationChannel channel,
    Map<String, Object> variables,
    Instant timestamp
) implements NotificationEvent {

    public GenericNotificationEvent(NotificationEventType templateCode, List<String> recipients, NotificationChannel channel, Map<String, Object> variables) {
        this(UUID.randomUUID(), templateCode, recipients, channel, variables, Instant.now());
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String getEventName() {
        return templateCode.name();
    }
}
