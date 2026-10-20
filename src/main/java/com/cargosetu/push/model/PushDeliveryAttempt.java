package com.cargosetu.push.model;

import com.cargosetu.abstracts.AbstractModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One result per target installation for one logical notification
 * (NotificationAudit row). NotificationAudit is one row per recipient per
 * channel, so it cannot represent "3 devices, 1 failed" on its own - this
 * table is the per-device granularity the plan's delivery audit calls for.
 */
@Entity
@Table(name = "push_delivery_attempt")
@Getter
@Setter
public class PushDeliveryAttempt extends AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Plain indexed column, not a formal FK - matches NotificationAudit's own style. */
    @Column(name = "audit_id", nullable = false)
    private Long auditId;

    @Column(name = "token_record_id", nullable = false)
    private Long tokenRecordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PushDeliveryStatus status;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "attempted_at")
    private LocalDateTime attemptedAt;
}
