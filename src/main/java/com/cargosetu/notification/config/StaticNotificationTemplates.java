package com.cargosetu.notification.config;

import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationEventType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Static, in-code templates for the non-EMAIL channels (SMS/PUSH/WHATSAPP), which are
 * all short, plain-text one-liners that don't warrant individual files the way EMAIL
 * bodies do (see {@code templates/email/events/}).
 */
public final class StaticNotificationTemplates {

    public record TemplateText(String subject, String body) {
    }

    private static final Map<String, TemplateText> TEMPLATES = new HashMap<>();

    static {
        put(NotificationEventType.CUSTOMER_CREATED, NotificationChannel.SMS,
                null,
                "Hello {{name}}, welcome to CargoSetu! Your account is active.");

        put(NotificationEventType.STAFF_CREATED, NotificationChannel.SMS,
                null,
                "Hello {{name}}, welcome to the team! Your username is {{username}}.");

        put(NotificationEventType.BOOKING_CREATED, NotificationChannel.SMS,
                null,
                "Booking confirmed: {{bookingNumber}}. Pickup: {{pickupLocation}}. Thanks for choosing CargoSetu!");

        put(NotificationEventType.OTP_SMS, NotificationChannel.SMS,
                null,
                "Your CargoSetu OTP is {{otpCode}}. Please do not share it with anyone.");

        put(NotificationEventType.TRIP_ASSIGNED_STAFF, NotificationChannel.PUSH,
                "Trip {{tripNumber}} assigned",
                "{{driverName}} is assigned to trip {{tripNumber}}: {{sourceCity}} -> {{destinationCity}}");

        put(NotificationEventType.CHAT_MESSAGE_RECEIVED, NotificationChannel.PUSH,
                "New message from {{senderName}}",
                "{{messagePreview}}");

        put(NotificationEventType.LOGIN, NotificationChannel.PUSH,
                "New login to your account",
                "Hello {{name}}, a new login to your CargoSetu account was detected on {{loginTime}} from {{device}}.");

        put(NotificationEventType.TRIP_START, NotificationChannel.PUSH,
                "Trip {{tripNumber}} started",
                "{{driverName}} has started trip {{tripNumber}}: {{sourceCity}} -> {{destinationCity}}");

        put(NotificationEventType.TRIP_END, NotificationChannel.PUSH,
                "Trip {{tripNumber}} completed",
                "Trip {{tripNumber}} ({{sourceCity}} -> {{destinationCity}}) has been completed.");

        put(NotificationEventType.DRIVER_AVAILABLE, NotificationChannel.PUSH,
                "Driver available near {{location}}",
                "A driver with vehicle {{vehicleNumber}} is now available near {{location}}.");

        put(NotificationEventType.LOAD_AVAILABLE, NotificationChannel.PUSH,
                "New load available: {{sourceCity}} -> {{destinationCity}}",
                "A new load is available from {{sourceCity}} to {{destinationCity}}. Tap to view details.");
    }

    private StaticNotificationTemplates() {
    }

    public static Optional<TemplateText> find(NotificationEventType eventType, NotificationChannel channel) {
        return Optional.ofNullable(TEMPLATES.get(key(eventType, channel)));
    }

    /** Exposes all entries for the admin read-only template listing. */
    public static Map<String, TemplateText> all() {
        return TEMPLATES;
    }

    private static void put(NotificationEventType eventType, NotificationChannel channel, String subject, String body) {
        TEMPLATES.put(key(eventType, channel), new TemplateText(subject, body));
    }

    private static String key(NotificationEventType eventType, NotificationChannel channel) {
        return eventType.name() + "|" + channel.name();
    }
}
