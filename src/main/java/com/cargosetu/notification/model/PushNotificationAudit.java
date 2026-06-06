package com.cargosetu.notification.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "push_notification_audit")
@Getter
@Setter
public class PushNotificationAudit extends NotificationAuditBase {

    private Long userId;

    private String deviceId;

    private String pushToken;

    private String title;

    @Lob
    private String body;

    private String providerMessageId;

    private String platform;

    private Boolean clicked;

    private LocalDateTime clickedAt;
}
