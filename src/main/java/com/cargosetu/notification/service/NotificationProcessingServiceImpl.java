package com.cargosetu.notification.service;

import com.cargosetu.notification.event.GenericNotificationEvent;
import com.cargosetu.notification.infrastructure.channel.NotificationChannelSender;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationStatus;
import com.cargosetu.notification.model.NotificationTemplate;
import com.cargosetu.notification.model.NotificationAudit;
import com.cargosetu.notification.repository.NotificationTemplateRepository;
import com.cargosetu.notification.repository.NotificationAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessingServiceImpl implements NotificationProcessingService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationAuditRepository auditRepository;
    private final TemplateRenderingService renderingService;
    private final List<NotificationChannelSender> channelSenders;

    @Override
    public void processEvent(GenericNotificationEvent event) {
        log.info("Processing generic notification event: {} (ID: {})", event.templateCode(), event.eventId());

        // 1. Fetch template from DB for this templateCode and channel
        NotificationTemplate template = templateRepository
                .findByTemplateCodeAndChannelAndIsActiveTrue(event.templateCode(), event.channel())
                .orElse(null);

        if (template == null) {
            log.error("Active notification template not found for code: {} and channel: {}", 
                    event.templateCode(), event.channel());
            return;
        }

        // 2. Render template subject and body
        String renderedSubject = renderingService.render(template.getSubjectTemplate(), event.variables());
        String renderedBody = renderingService.render(template.getBodyTemplate(), event.variables());

        // 3. For each recipient, create a PENDING audit record and send
        for (String recipient : event.recipients()) {
            if (recipient == null || recipient.isBlank()) {
                continue;
            }

            // Sanitize recipient: trim whitespaces
            String cleanRecipient = recipient.trim();

            // Ensure SMS recipient is in international E.164 format (starts with '+')
            if (event.channel() == NotificationChannel.SMS && !cleanRecipient.startsWith("+")) {
                cleanRecipient = "+" + cleanRecipient;
            }

            NotificationAudit audit = new NotificationAudit();
            audit.setEventId(event.eventId());
            audit.setRecipient(cleanRecipient);
            audit.setChannel(event.channel());
            audit.setTemplateCode(event.templateCode());
            audit.setSubject(renderedSubject);
            audit.setMessageBody(renderedBody);
            audit.setStatus(NotificationStatus.PENDING);
            audit.setRetryCount(0);

            audit = auditRepository.save(audit);

            // 4. Find the strategy sender
            NotificationChannelSender sender = channelSenders.stream()
                    .filter(s -> s.supports(event.channel()))
                    .findFirst()
                    .orElse(null);

            if (sender == null) {
                String errorMsg = "No sender found supporting channel: " + event.channel();
                log.error(errorMsg);
                audit.setStatus(NotificationStatus.FAILED);
                audit.setFailureReason(errorMsg);
                auditRepository.save(audit);
                continue;
            }

            // 5. Send with retry delegate
            try {
                sender.send(audit);
            } catch (Exception e) {
                log.error("Failed to send notification to {} after retries: {}", cleanRecipient, e.getMessage());
            }
        }
    }
}
