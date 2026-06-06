package com.cargosetu.notification.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a customer is successfully created.
 */
public record CustomerCreatedEvent(
    UUID eventId,
    Instant timestamp,
    Long customerId,
    String email,
    String fullName,
    String mobileNo
) implements NotificationEvent {

    public CustomerCreatedEvent(Long customerId, String email, String fullName, String mobileNo) {
        this(UUID.randomUUID(), Instant.now(), customerId, email, fullName, mobileNo);
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
        return "CUSTOMER_CREATED";
    }
}
