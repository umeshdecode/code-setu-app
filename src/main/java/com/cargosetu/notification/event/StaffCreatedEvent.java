package com.cargosetu.notification.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a new staff member is successfully created.
 */
public record StaffCreatedEvent(
    UUID eventId,
    Instant timestamp,
    Long staffId,
    String email,
    String fullName,
    String userName
) implements NotificationEvent {

    public StaffCreatedEvent(Long staffId, String email, String fullName, String userName) {
        this(UUID.randomUUID(), Instant.now(), staffId, email, fullName, userName);
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
        return "STAFF_CREATED";
    }
}
