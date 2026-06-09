package com.cargosetu.notification.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a new customer inquiry is submitted.
 */
public record InquiryCreatedEvent(
    UUID eventId,
    Instant timestamp,
    String inquiryNumber,
    String name,
    String email,
    String phone,
    String subject,
    String message
) implements NotificationEvent {

    public InquiryCreatedEvent(String inquiryNumber, String name, String email, String phone, String subject, String message) {
        this(UUID.randomUUID(), Instant.now(), inquiryNumber, name, email, phone, subject, message);
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
        return "INQUIRY_CREATED";
    }
}
