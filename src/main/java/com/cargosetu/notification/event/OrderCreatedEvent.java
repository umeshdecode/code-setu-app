package com.cargosetu.notification.event;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
/**
 * Event published when an order is created.
 */
public record OrderCreatedEvent(
        UUID eventId,
        Instant timestamp,
        String orderId,
        String customerEmail,
        String customerPhone,
        String customerName,
        BigDecimal totalAmount
) implements NotificationEvent {
    public OrderCreatedEvent(String orderId, String customerEmail, String customerPhone, String customerName, BigDecimal totalAmount) {
        this(UUID.randomUUID(), Instant.now(), orderId, customerEmail, customerPhone, customerName, totalAmount);
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
        return "ORDER_CREATED";
    }
}