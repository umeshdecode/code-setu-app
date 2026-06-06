package com.cargosetu.notification.service;

import com.cargosetu.notification.event.NotificationEvent;
import com.cargosetu.notification.model.NotificationChannel;

public interface NotificationProvider {
    
    /**
     * Sends the notification to the target recipient.
     *
     * @param event     the business event
     * @param recipient the recipient's identifier (email address, phone number, etc.)
     */
    void send(NotificationEvent event, String recipient);

    /**
     * Indicates whether this provider supports the given notification channel.
     *
     * @param channel the channel to verify
     * @return true if supported, false otherwise
     */
    boolean supports(NotificationChannel channel);
}
