package com.cargosetu.notification.service;

import com.cargosetu.notification.config.StaticNotificationTemplates;
import com.cargosetu.notification.config.StaticNotificationTemplates.TemplateText;
import com.cargosetu.notification.event.GenericNotificationEvent;
import com.cargosetu.notification.infrastructure.channel.NotificationChannelSender;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationStatus;
import com.cargosetu.notification.model.NotificationAudit;
import com.cargosetu.notification.repository.NotificationAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessingServiceImpl implements NotificationProcessingService {

    private final NotificationAuditRepository auditRepository;
    private final TemplateRenderingService renderingService;
    private final List<NotificationChannelSender> channelSenders;

    @Override
    public void processEvent(GenericNotificationEvent event) {
        log.info("Processing generic notification event: {} (ID: {})", event.templateCode(), event.eventId());

        String renderedSubject;
        String renderedBody;

        if (event.channel() == NotificationChannel.EMAIL) {
            // EMAIL templates live on disk under templates/email/events/ (falls back to
            // fallback.html internally if the event has no file of its own).
            try {
                TemplateRenderingService.RenderedNotification rendered =
                        renderingService.renderEmailForEvent(event.templateCode(), event.variables());
                renderedSubject = rendered.subject();
                renderedBody = rendered.body();
            } catch (Exception e) {
                log.warn("Failed to render EMAIL template for {}: {} — using a generic fallback message",
                        event.templateCode(), e.getMessage());
                renderedSubject = "Notification: " + event.templateCode().humanize();
                renderedBody = "You have a new notification regarding " + event.templateCode().humanize() + ".";
            }
        } else {
            // SMS/PUSH/WHATSAPP templates are short, static plain-text entries.
            TemplateText template = StaticNotificationTemplates.find(event.templateCode(), event.channel()).orElse(null);
            if (template != null) {
                try {
                    renderedSubject = renderingService.renderSubject(template.subject(), event.variables(), event.channel());
                    renderedBody = renderingService.render(template.body(), event.variables(), event.channel());
                } catch (Exception e) {
                    log.warn("Failed to render template for {} ({}): {} — falling back to plain placeholder substitution",
                            event.templateCode(), event.channel(), e.getMessage());
                    renderedSubject = renderingService.render(template.subject(), event.variables());
                    renderedBody = renderingService.render(template.body(), event.variables());
                }
            } else {
                log.warn("No static template found for {} ({}) — using a generic fallback message",
                        event.templateCode(), event.channel());
                renderedSubject = null;
                renderedBody = "You have a new notification regarding " + event.templateCode().humanize() + ".";
            }
        }

        log.info("Final rendered template for {} ({}) before send:\nSubject: {}\nBody:\n{}",
                event.templateCode(), event.channel(), renderedSubject, renderedBody);

        // For each recipient, create a PENDING audit record and send
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
            audit.setTemplateCode(event.templateCode().name());
            audit.setSubject(renderedSubject);
            audit.setMessageBody(renderedBody);
            audit.setStatus(NotificationStatus.PENDING);
            audit.setRetryCount(0);

            audit = auditRepository.save(audit);

            // Find the strategy sender
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

            // Send with retry delegate
            try {
                sender.send(audit);

                if (audit.getStatus() == NotificationStatus.SENT) {
                    log.info("Successfully sent {} ({}) to {}. Final template sent:\nSubject: {}\nBody:\n{}",
                            event.templateCode(), event.channel(), cleanRecipient,
                            audit.getSubject(), audit.getMessageBody());
                }
            } catch (Exception e) {
                log.error("Failed to send notification to {} after retries: {}", cleanRecipient, e.getMessage());
            }
        }
    }
}
