package com.cargosetu.notification.controller;

import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationAudit;
import com.cargosetu.notification.repository.NotificationAuditRepository;
import com.cargosetu.notification.service.DemoBusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/demo-notifications")
@RequiredArgsConstructor
public class DemoNotificationController {

    private final DemoBusinessService businessService;
    private final NotificationAuditRepository auditRepository;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(
            @RequestParam String email,
            @RequestParam String fullName,
            @RequestParam String username,
            @RequestParam(defaultValue = "false") boolean shouldRollback) {
        
        try {
            businessService.registerUser(email, fullName, username, shouldRollback);
            return ResponseEntity.ok("User registration business transaction committed successfully! " +
                    "Check background thread logs to verify that asynchronous notifications were triggered.");
        } catch (Exception e) {
            log.error("Registration endpoint caught exception: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Business transaction failed and rolled back: " + e.getMessage() + 
                    ". Check database logs to verify that no notifications were sent.");
        }
    }

    @PostMapping("/order")
    public ResponseEntity<String> createOrder(
            @RequestParam String orderId,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String name,
            @RequestParam double amount) {
        
        businessService.createOrder(orderId, email, phone, name, BigDecimal.valueOf(amount));
        return ResponseEntity.ok("Order creation business transaction committed successfully! " +
                 "Check background thread logs to verify SMS and Email notifications.");
    }

    @GetMapping("/email-logs")
    public ResponseEntity<List<NotificationAudit>> getEmailLogs() {
        List<NotificationAudit> emailLogs = auditRepository.findAll().stream()
                .filter(a -> a.getChannel() == NotificationChannel.EMAIL)
                .toList();
        return ResponseEntity.ok(emailLogs);
    }

    @GetMapping("/sms-logs")
    public ResponseEntity<List<NotificationAudit>> getSmsLogs() {
        List<NotificationAudit> smsLogs = auditRepository.findAll().stream()
                .filter(a -> a.getChannel() == NotificationChannel.SMS)
                .toList();
        return ResponseEntity.ok(smsLogs);
    }

    @GetMapping("/push-logs")
    public ResponseEntity<List<NotificationAudit>> getPushLogs() {
        List<NotificationAudit> pushLogs = auditRepository.findAll().stream()
                .filter(a -> a.getChannel() == NotificationChannel.PUSH)
                .toList();
        return ResponseEntity.ok(pushLogs);
    }
}
