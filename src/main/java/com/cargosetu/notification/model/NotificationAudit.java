package com.cargosetu.notification.model;

import com.cargosetu.abstracts.AbstractModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "notification_audit")
@Getter
@Setter
public class NotificationAudit extends AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private NotificationChannel channel;

    @Column(name = "template_code", nullable = false)
    private String templateCode;

    @Column(name = "subject")
    private String subject;

    @Lob
    @Column(name = "message_body", nullable = false, columnDefinition = "LONGTEXT")
    private String messageBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Lob
    @Column(name = "failure_reason", columnDefinition = "LONGTEXT")
    private String failureReason;

    @Column(name = "sent_at")
    private java.time.LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private java.time.LocalDateTime deliveredAt;

    /**
     * Copied from the resolved NotificationTemplate at audit-creation time so channel senders
     * (e.g. EmailChannelSender) know how to render the body without re-fetching the template.
     */
    @Column(name = "is_html")
    private Boolean isHtml = false;

    /**
     * Optional business-level identifier (e.g. a Trip ID) carried over from
     * GenericNotificationEvent#referenceId, used to deduplicate repeat notifications for the
     * same recipient about the same business entity (see NotificationAuditRepository).
     */
    @Column(name = "reference_id", length = 100)
    private String referenceId;
}
