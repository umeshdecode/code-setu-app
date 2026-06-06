package com.cargosetu.notification.service;

import com.cargosetu.notification.event.NotificationEvent;
import com.cargosetu.notification.model.NotificationChannel;

public interface NotificationDispatcher {
    
    /**
     * Dispatches a notification to a specific channel and recipient.
     *
     * @param event     the business event triggering the notification
     * @param channel   the channel to send through (Email, SMS, etc.)
     * @param recipient the recipient's destination
     */
    void dispatch(NotificationEvent event, NotificationChannel channel, String recipient);
}
