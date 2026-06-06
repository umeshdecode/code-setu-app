package com.cargosetu.notification.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sms_notification_audit")
@Getter
@Setter
public class SmsNotificationAudit extends NotificationAuditBase {

    private String mobileNumber;

    @Lob
    private String message;

    private String providerMessageId;

    private String dltTemplateId;

    private String dltEntityId;

    private Integer messageLength;

    private Integer numberOfParts;
}
