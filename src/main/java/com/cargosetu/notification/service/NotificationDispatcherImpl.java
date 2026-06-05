package com.cargosetu.notification.service;

import com.cargosetu.notification.event.NotificationEvent;
import com.cargosetu.notification.model.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcherImpl implements NotificationDispatcher {

    private final List<NotificationProvider> providers;
    private final NotificationLogService logService;

    @Override
    public void dispatch(NotificationEvent event, NotificationChannel channel, String recipient) {
        log.info("Dispatching event {} (ID: {}) to channel: {} for recipient: {}", 
                event.getEventName(), event.getEventId(), channel, recipient);

        // 1. Create a PENDING log entry in the database immediately
        logService.createPendingLog(event.getEventId(), event.getEventName(), channel, recipient);

        // 2. Find the provider that supports the target channel
        NotificationProvider provider = providers.stream()
                .filter(p -> p.supports(channel))
                .findFirst()
                .orElse(null);

        if (provider == null) {
            String errorMsg = "No provider found supporting channel: " + channel;
            log.error(errorMsg);
            logService.logFailure(event.getEventId(), channel, recipient, errorMsg);
            return;
        }

        // 3. Delegate execution to the provider (this call triggers @Retryable logic)
        try {
            provider.send(event, recipient);
        } catch (Exception e) {
            // Fallback catch. The provider's @Recover should normally handle failures,
            // but we log here just in case a non-transient or uncaught exception slips through.
            log.error("Fatal exception during dispatch to {}: {}", channel, e.getMessage(), e);
            logService.logFailure(event.getEventId(), channel, recipient, "Uncaught error: " + e.getMessage());
        }
    }
}
