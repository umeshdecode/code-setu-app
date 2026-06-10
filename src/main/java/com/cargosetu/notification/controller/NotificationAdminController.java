package com.cargosetu.notification.controller;

import com.cargosetu.notification.event.GenericNotificationEvent;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationTemplate;
import com.cargosetu.notification.model.NotificationAudit;
import com.cargosetu.notification.repository.NotificationTemplateRepository;
import com.cargosetu.notification.repository.NotificationAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class NotificationAdminController {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationAuditRepository auditRepository;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping("/templates")
    public ResponseEntity<List<NotificationTemplate>> getAllTemplates() {
        return ResponseEntity.ok(templateRepository.findAll());
    }

    @PostMapping("/templates")
    public ResponseEntity<NotificationTemplate> createTemplate(@RequestBody NotificationTemplate template) {
        log.info("Creating new notification template for code: {} and channel: {}", 
                template.getTemplateCode(), template.getChannel());
        return ResponseEntity.ok(templateRepository.save(template));
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<NotificationTemplate> updateTemplate(
            @PathVariable Long id, 
            @RequestBody NotificationTemplate templateDetails) {
        
        log.info("Updating template ID: {}", id);
        Optional<NotificationTemplate> optionalTemplate = templateRepository.findById(id);
        
        if (optionalTemplate.isPresent()) {
            NotificationTemplate template = optionalTemplate.get();
            template.setSubjectTemplate(templateDetails.getSubjectTemplate());
            template.setBodyTemplate(templateDetails.getBodyTemplate());
            template.setIsActive(templateDetails.getIsActive());
            return ResponseEntity.ok(templateRepository.save(template));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/audits")
    public ResponseEntity<List<NotificationAudit>> getAllAudits() {
        return ResponseEntity.ok(auditRepository.findAll());
    }

    @PostMapping("/send-test")
    public ResponseEntity<String> sendTestNotification(
            @RequestParam String templateCode,
            @RequestParam String recipient,
            @RequestParam NotificationChannel channel,
            @RequestBody Map<String, Object> variables) {
        
        log.info("Dispatching manual test notification for template: {} to: {}", templateCode, recipient);
        
        GenericNotificationEvent event = new GenericNotificationEvent(
                templateCode,
                List.of(recipient),
                channel,
                variables
        );
        eventPublisher.publishEvent(event);
        
        return ResponseEntity.ok("Test notification event published successfully! Event ID: " + event.eventId());
    }
}
