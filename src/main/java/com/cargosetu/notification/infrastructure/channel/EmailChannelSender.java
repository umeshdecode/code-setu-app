package com.cargosetu.notification.infrastructure.channel;

import com.cargosetu.exception.NotificationDeliveryException;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationStatus;
import com.cargosetu.notification.model.NotificationAudit;
import com.cargosetu.notification.repository.NotificationAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailChannelSender implements NotificationChannelSender {

    private final JavaMailSender mailSender;
    private final NotificationAuditRepository auditRepository;

    @Value("${app.notification.email.from}")
    private String fromEmail;

    @Value("${app.notification.email.mock}")
    private boolean mockEnabled;

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    @Retryable(
        retryFor = { Exception.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )
    public void send(NotificationAudit audit) throws Exception {
        String recipient = audit.getRecipient();
        
        // Log the current attempt to DB
        audit.setRetryCount(audit.getRetryCount() + 1);
        audit.setStatus(audit.getRetryCount() <= 1 ? NotificationStatus.PROCESSING : NotificationStatus.RETRYING);
        auditRepository.save(audit);

        int currentAttempt = audit.getRetryCount();
        log.info("[Email Channel] Sending email to: {} (Attempt #{})", recipient, currentAttempt);

        // Failures simulation logic for testing
        if (recipient.contains("permanent-fail")) {
            log.warn("Simulating permanent email failure on attempt #{}", currentAttempt);
            throw new NotificationDeliveryException("SMTP server connection timeout (Permanent Simulated Failure)");
        } else if (recipient.contains("transient-fail") && currentAttempt < 3) {
            log.warn("Simulating transient email failure on attempt #{}", currentAttempt);
            throw new NotificationDeliveryException("Temporary SMTP server throttling (Transient Simulated Failure)");
        }

        if (mockEnabled) {
            log.info("[MOCK EMAIL] Dispatching email to: {}. Subject: '{}'", recipient, audit.getSubject());
            Thread.sleep(300); // Simulate network latency
        } else {
            try {
                log.info("[REAL EMAIL] Dispatching physical SMTP email to: {} via JavaMailSender", recipient);
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(recipient);
                message.setSubject(audit.getSubject());
                message.setText(audit.getMessageBody());
                
                mailSender.send(message);
            } catch (Exception e) {
                log.error("Failed to send SMTP email to: {}", recipient, e);
                throw new NotificationDeliveryException("SMTP client error: " + e.getMessage(), e);
            }
        }

        // Success!
        audit.setStatus(NotificationStatus.SENT);
        audit.setSentAt(LocalDateTime.now());
        audit.setDeliveredAt(LocalDateTime.now());
        auditRepository.save(audit);
        log.info("[Email Channel] Email sent successfully to: {}", recipient);
    }

    @Recover
    public void recover(Exception e, NotificationAudit audit) {
        log.error("[Email Channel] All email retry attempts exhausted for: {}. Failing gracefully. Reason: {}", 
                audit.getRecipient(), e.getMessage());
        audit.setStatus(NotificationStatus.FAILED);
        audit.setFailureReason(e.getMessage());
        auditRepository.save(audit);
    }

    public void sendEmailWithAttachment(String to, String subject, String body, byte[] attachment, String attachmentName) throws Exception {
        if (mockEnabled) {
            log.info("[MOCK EMAIL] Dispatching email with attachment to: {}. Subject: '{}'", to, subject);
            Thread.sleep(300);
            return;
        }
        log.info("[REAL EMAIL] Dispatching email with attachment to: {} via JavaMailSender", to);
        var message = mailSender.createMimeMessage();
        org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true);
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body);
        helper.addAttachment(attachmentName, new org.springframework.core.io.ByteArrayResource(attachment));
        
        mailSender.send(message);
    }
}
