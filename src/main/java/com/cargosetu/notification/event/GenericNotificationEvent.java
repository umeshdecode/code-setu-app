package com.cargosetu.notification.event;

import com.cargosetu.notification.model.NotificationChannel;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GenericNotificationEvent(
    UUID eventId,
    String templateCode,
    List<String> recipients,
    NotificationChannel channel,
    Map<String, Object> variables,
    Instant timestamp,
    String referenceId
) implements NotificationEvent {

    public GenericNotificationEvent(String templateCode, List<String> recipients, NotificationChannel channel, Map<String, Object> variables) {
        this(UUID.randomUUID(), templateCode, recipients, channel, variables, Instant.now(), null);
    }

    /**
     * Overload that carries a business-level referenceId (e.g. a Trip ID) so downstream
     * processing can deduplicate notifications per-recipient-per-reference (see
     * NotificationAudit#referenceId and NotificationAuditRepository's dedup query).
     */
    public GenericNotificationEvent(String templateCode, List<String> recipients, NotificationChannel channel, Map<String, Object> variables, String referenceId) {
        this(UUID.randomUUID(), templateCode, recipients, channel, variables, Instant.now(), referenceId);
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
        return templateCode;
    }
}
