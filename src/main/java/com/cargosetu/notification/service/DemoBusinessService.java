package com.cargosetu.notification.service;

import com.cargosetu.notification.event.OrderCreatedEvent;
import com.cargosetu.notification.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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

        // Publish in-memory event
        UserRegisteredEvent event = new UserRegisteredEvent(101L, email, fullName, username);
        eventPublisher.publishEvent(event);
        log.info("Published UserRegisteredEvent. Transaction still active.");

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

        OrderCreatedEvent event = new OrderCreatedEvent(orderId, customerEmail, customerPhone, customerName, amount);
        eventPublisher.publishEvent(event);
        log.info("Published OrderCreatedEvent. Transaction still active.");
    }
}
