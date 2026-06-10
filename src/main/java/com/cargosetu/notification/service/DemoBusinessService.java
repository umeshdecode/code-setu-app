package com.cargosetu.notification.service;

import com.cargosetu.notification.event.GenericNotificationEvent;
import com.cargosetu.notification.model.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoBusinessService {

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void registerUser(String email, String fullName, String username, boolean shouldRollback) {
        log.info("[Business Thread: {}] Starting user registration transaction for: {}", 
                Thread.currentThread().getName(), email);
        
        // Simulating database write
        log.info("Saving user to database...");

        // Publish in-memory generic event
        Map<String, Object> variables = Map.of(
            "customerId", 101L,
            "name", fullName != null ? fullName : "",
            "email", email != null ? email : "",
            "mobileNo", "+19999999999"
        );
        GenericNotificationEvent event = new GenericNotificationEvent("CUSTOMER_CREATED", List.of(email), NotificationChannel.EMAIL, variables);
        eventPublisher.publishEvent(event);
        log.info("Published GenericNotificationEvent for CUSTOMER_CREATED. Transaction still active.");

        if (shouldRollback) {
            log.warn("Simulating transaction rollback!");
            throw new RuntimeException("Simulated Database Transaction Rollback Exception");
        }
        
        log.info("User registration transaction finished successfully. Committing transaction...");
    }

    @Transactional
    public void createOrder(String orderId, String customerEmail, String customerPhone, String customerName, BigDecimal amount) {
        log.info("[Business Thread: {}] Starting order creation transaction for Order ID: {}", 
                Thread.currentThread().getName(), orderId);

        log.info("Saving order to database...");

        Map<String, Object> variables = Map.of(
            "name", customerName != null ? customerName : "",
            "bookingNumber", orderId != null ? orderId : "",
            "pickupLocation", "Warehouse A",
            "deliveryLocation", "Warehouse B",
            "amount", amount != null ? amount.toString() : "0.0"
        );

        if (customerEmail != null && !customerEmail.isBlank()) {
            eventPublisher.publishEvent(new GenericNotificationEvent("BOOKING_CREATED", List.of(customerEmail), NotificationChannel.EMAIL, variables));
        }
        if (customerPhone != null && !customerPhone.isBlank()) {
            eventPublisher.publishEvent(new GenericNotificationEvent("BOOKING_CREATED", List.of(customerPhone), NotificationChannel.SMS, variables));
        }
        log.info("Published GenericNotificationEvent for BOOKING_CREATED. Transaction still active.");
    }
}
