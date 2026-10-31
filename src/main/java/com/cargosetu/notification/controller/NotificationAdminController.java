package com.cargosetu.notification.controller;

import com.cargosetu.notification.config.StaticNotificationTemplates;
import com.cargosetu.notification.event.GenericNotificationEvent;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationEventType;
import com.cargosetu.notification.model.NotificationAudit;
import com.cargosetu.notification.repository.NotificationAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class NotificationAdminController {

    private static final String EMAIL_EVENTS_CLASSPATH = "classpath*:templates/email/events/*.html";
    private static final String SUBJECT_PREFIX = "Subject:";

    private final NotificationAuditRepository auditRepository;
    private final ApplicationEventPublisher eventPublisher;

    public record TemplateInfo(String templateCode, String channel, String subject, String source) {
    }

    /**
     * Read-only listing of the notification templates this app actually uses: the EMAIL
     * files under {@code templates/email/events/} and the static SMS/PUSH/WHATSAPP
     * entries in {@link StaticNotificationTemplates}. There is no DB-backed CRUD for
     * templates anymore — edit the files / the static config directly instead.
     */
    @GetMapping("/templates")
    public ResponseEntity<List<TemplateInfo>> getAllTemplates() {
        List<TemplateInfo> templates = new ArrayList<>();
        templates.addAll(listEmailFileTemplates());
        templates.addAll(listStaticTemplates());
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/audits")
    public ResponseEntity<List<NotificationAudit>> getAllAudits() {
        return ResponseEntity.ok(auditRepository.findAll());
    }

    @PostMapping("/send-test")
    public ResponseEntity<String> sendTestNotification(
            @RequestParam NotificationEventType templateCode,
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

    private List<TemplateInfo> listEmailFileTemplates() {
        List<TemplateInfo> result = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(EMAIL_EVENTS_CLASSPATH);
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null || filename.equalsIgnoreCase("fallback.html")) {
                    continue;
                }
                String eventCode = filename.substring(0, filename.length() - ".html".length()).toUpperCase();
                String subject = readFirstSubjectLine(resource);
                result.add(new TemplateInfo(eventCode, NotificationChannel.EMAIL.name(), subject, "FILE"));
            }
        } catch (IOException e) {
            log.error("Failed to list EMAIL event templates from classpath", e);
        }
        return result;
    }

    private String readFirstSubjectLine(Resource resource) {
        try (InputStream in = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (firstLine != null && firstLine.trim().startsWith(SUBJECT_PREFIX)) {
                return firstLine.trim().substring(SUBJECT_PREFIX.length()).trim();
            }
        } catch (IOException e) {
            log.warn("Failed to read subject line from template file: {}", resource.getFilename(), e);
        }
        return null;
    }

    private List<TemplateInfo> listStaticTemplates() {
        List<TemplateInfo> result = new ArrayList<>();
        StaticNotificationTemplates.all().forEach((key, value) -> {
            String[] parts = key.split("\\|", 2);
            String eventCode = parts[0];
            String channel = parts.length > 1 ? parts[1] : "";
            result.add(new TemplateInfo(eventCode, channel, value.subject(), "STATIC"));
        });
        return result;
    }
}
