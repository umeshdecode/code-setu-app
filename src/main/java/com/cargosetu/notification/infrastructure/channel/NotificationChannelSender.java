package com.cargosetu.notification.infrastructure.channel;

import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationAudit;

public interface NotificationChannelSender {
    
    /**
     * Checks if this sender supports the specified notification channel.
     *
     * @param channel the target notification channel
     * @return true if supported, false otherwise
     */
    boolean supports(NotificationChannel channel);

    /**
     * Sends the notification content defined in the audit record physically or mock-delivers it.
     *
     * @param audit the audited notification entry containing recipient, subject, body, etc.
     * @throws Exception if sending fails
     */
    void send(NotificationAudit audit) throws Exception;
}
