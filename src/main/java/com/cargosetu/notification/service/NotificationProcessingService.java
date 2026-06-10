package com.cargosetu.notification.service;

import com.cargosetu.notification.event.GenericNotificationEvent;

public interface NotificationProcessingService {

    /**
     * Processes a generic notification event by compiling template placeholders,
     * writing audit records, and sending notifications through the configured channel adapters.
     * Includes built-in Spring Retry handling for transient failures.
     *
     * @param event the event triggering notifications
     */
    void processEvent(GenericNotificationEvent event);
}
