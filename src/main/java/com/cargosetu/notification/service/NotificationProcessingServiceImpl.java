package com.cargosetu.notification.service;

import com.cargosetu.notification.event.GenericNotificationEvent;
import com.cargosetu.notification.infrastructure.channel.NotificationChannelSender;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationEventType;
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

        String renderedSubject;
        String renderedBody;

        if (template != null) {
            // 2a. Normal path: render the DB-stored subject/body for this event+channel.
            //     Fall back to plain {{var}} substitution if real rendering throws, so a
            //     bad Thymeleaf expression never drops the notification entirely.
            try {
                renderedSubject = renderingService.renderSubject(template.getSubjectTemplate(), event.variables(), event.channel());
                renderedBody = renderingService.render(template.getBodyTemplate(), event.variables(), event.channel());
            } catch (Exception e) {
                log.warn("Failed to render template for {} ({}): {} — falling back to plain placeholder substitution",
                        event.templateCode(), event.channel(), e.getMessage());
                renderedSubject = renderingService.render(template.getSubjectTemplate(), event.variables());
                renderedBody = renderingService.render(template.getBodyTemplate(), event.variables());
            }
        } else {
            // 2b. No active DB template for this event+channel: don't silently drop the
            //     notification — fall back to a generic rendering so it still gets sent
            //     and audited.
            log.warn("No active notification template found for code: {} and channel: {} — using fallback template",
                    event.templateCode(), event.channel());
            if (event.channel() == NotificationChannel.EMAIL) {
                renderedSubject = "Notification: " + humanize(event.templateCode());
                renderedBody = renderingService.renderEventFileTemplate(event.templateCode(), event.variables());
            } else {
                renderedSubject = null;
                renderedBody = "You have a new notification regarding " + humanize(event.templateCode()) + ".";
            }
        }

        log.info("Final rendered template for {} ({}) before send:\nSubject: {}\nBody:\n{}",
                event.templateCode(), event.channel(), renderedSubject, renderedBody);

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
            audit.setTemplateCode(event.templateCode().name());
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

    private String humanize(NotificationEventType eventType) {
        String[] words = eventType.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
