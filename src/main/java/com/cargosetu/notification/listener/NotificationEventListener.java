package com.cargosetu.notification.listener;

import com.cargosetu.notification.event.OrderCreatedEvent;
import com.cargosetu.notification.event.StaffCreatedEvent;
import com.cargosetu.notification.event.UserRegisteredEvent;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationDispatcher dispatcher;

    /**
     * Listens for UserRegisteredEvent. Executed AFTER the database transaction successfully commits.
     * Runs asynchronously on a dedicated thread pool thread.
     */
    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("[Listener Thread: {}] Intercepted UserRegisteredEvent (ID: {}). Starting dispatch.",
                Thread.currentThread().getName(), event.eventId());

        // Dispatch email notification
        if (event.email() != null && !event.email().isBlank()) {
            dispatcher.dispatch(event, NotificationChannel.EMAIL, event.email());
        }

        // Dispatch SMS notification (using username or simulated phone number)
        String smsRecipient = "+19999999999"; // Default simulated number
        dispatcher.dispatch(event, NotificationChannel.SMS, smsRecipient);
    }

    /**
     * Listens for OrderCreatedEvent. Executed AFTER the database transaction successfully commits.
     * Runs asynchronously on a dedicated thread pool thread.
     */
    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("[Listener Thread: {}] Intercepted OrderCreatedEvent (ID: {}). Starting dispatch.",
                Thread.currentThread().getName(), event.eventId());

        // Dispatch email notification
        if (event.customerEmail() != null && !event.customerEmail().isBlank()) {
            dispatcher.dispatch(event, NotificationChannel.EMAIL, event.customerEmail());
        }

        // Dispatch SMS notification
        if (event.customerPhone() != null && !event.customerPhone().isBlank()) {
            dispatcher.dispatch(event, NotificationChannel.SMS, event.customerPhone());
        }
    }

    /**
     * Listens for StaffCreatedEvent. Executed AFTER the database transaction successfully commits.
     * Runs asynchronously on a dedicated thread pool thread.
     */
    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleStaffCreatedEvent(StaffCreatedEvent event) {
        log.info("[Listener Thread: {}] Intercepted StaffCreatedEvent (ID: {}). Starting dispatch.",
                Thread.currentThread().getName(), event.eventId());

        // Dispatch email notification to staff email address
        if (event.email() != null && !event.email().isBlank()) {
            dispatcher.dispatch(event, NotificationChannel.EMAIL, event.email());
        }
    }
}
