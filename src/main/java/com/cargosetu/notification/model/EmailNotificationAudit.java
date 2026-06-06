package com.cargosetu.notification.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_notification_audit")
@Getter
@Setter
public class EmailNotificationAudit extends NotificationAuditBase {

    private String recipientEmail;

    private String cc;

    private String bcc;

    private String subject;

    @Lob
    private String emailBody;

    private String providerMessageId;

    private Boolean delivered;

    private LocalDateTime openedAt;

    private Boolean attachmentPresent;
}
