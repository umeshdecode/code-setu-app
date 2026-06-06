package com.cargosetu.notification.model;

import com.cargosetu.abstracts.AbstractModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
public abstract class NotificationAuditBase extends AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventCode;

    private String referenceId;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private LocalDateTime sentAt;

    private LocalDateTime deliveredAt;

    private Integer retryCount = 0;

    @Lob
    private String failureReason;

    @Lob
    private String providerResponse;

    private String providerName;
}
