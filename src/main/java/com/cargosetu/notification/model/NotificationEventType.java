package com.cargosetu.notification.model;

public enum NotificationEventType {
    CUSTOMER_CREATED,
    CUSTOMER_UPDATED,
    INQUIRY_CREATED,
    INQUIRY_CREATED_SUPPORT,
    INQUIRY_REPLY,
    STAFF_CREATED,
    VEHICLE_ASSIGNED,
    BOOKING_CREATED,
    INVOICE_GENERATED,
    PAYMENT_RECEIVED,
    OTP_SMS,
    TRIP_ASSIGNED_STAFF,
    CHAT_MESSAGE_RECEIVED,
    LOGIN,
    REGISTRATION,
    TRIP_START,
    TRIP_END,
    DRIVER_AVAILABLE,
    LOAD_AVAILABLE;

    /**
     * Human-readable form of this event, e.g. {@code STAFF_CREATED} -> {@code "Staff Created"}.
     * Used for generic fallback subjects/messages when no template exists for an event.
     */
    public String humanize() {
        String[] words = name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
