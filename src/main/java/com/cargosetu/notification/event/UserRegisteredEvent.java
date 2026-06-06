package com.cargosetu.notification.event;
import java.time.Instant;
import java.util.UUID;
/**
 * Event published when a new user registers.
 */
public record UserRegisteredEvent(
        UUID eventId,
        Instant timestamp,
        Long userId,
        String email,
        String fullName,
        String username
) implements NotificationEvent {

    public UserRegisteredEvent(Long userId, String email, String fullName, String username) {
        this(UUID.randomUUID(), Instant.now(), userId, email, fullName, username);
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
        return "USER_REGISTERED";
    }
}

