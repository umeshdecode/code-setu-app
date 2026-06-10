package com.cargosetu.notification.listener;

import com.cargosetu.notification.event.GenericNotificationEvent;
import com.cargosetu.notification.service.NotificationProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenericNotificationEventListener {

    private final NotificationProcessingService processingService;

    /**
     * Listens for GenericNotificationEvent. Executed asynchronously on a dedicated thread pool
     * thread AFTER the active database transaction commits.
     *
     * @param event the published notification event
     */
    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleGenericNotificationEvent(GenericNotificationEvent event) {
        log.info("[Generic Listener Thread: {}] Intercepted GenericNotificationEvent (ID: {}). starting processing.",
                Thread.currentThread().getName(), event.eventId());
        try {
            processingService.processEvent(event);
        } catch (Exception e) {
            log.error("Failed to process notification event: {}", event.eventId(), e);
        }
    }
}
