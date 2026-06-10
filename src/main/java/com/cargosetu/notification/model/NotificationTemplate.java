package com.cargosetu.notification.model;

import com.cargosetu.abstracts.AbstractModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_template", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"template_code", "channel"})
})
@Getter
@Setter
public class NotificationTemplate extends AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_code", nullable = false)
    private String templateCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private NotificationChannel channel;

    @Column(name = "subject_template")
    private String subjectTemplate;

    @Lob
    @Column(name = "body_template", nullable = false, columnDefinition = "LONGTEXT")
    private String bodyTemplate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
