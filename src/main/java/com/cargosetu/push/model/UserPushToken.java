package com.cargosetu.push.model;

import com.cargosetu.abstracts.AbstractModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One row per device/browser installation for one account. A single account can
 * have many active rows (multi-device); AbstractModel's isActive doubles as
 * "eligible for sending" - deactivated rather than deleted, e.g. when FCM
 * reports the token as unregistered.
 */
@Entity
@Table(name = "tblm_user_push_token", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"principal_type", "principal_id", "device_id"})
})
@Getter
@Setter
public class UserPushToken extends AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type", nullable = false)
    private PrincipalType principalType;

    @Column(name = "principal_id", nullable = false)
    private Long principalId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    // 512 chars comfortably covers any real FCM token (~150-250 chars observed) while staying
    // under MySQL's 3072-byte max index key length at utf8mb4 for this uniquely-indexed column.
    @Column(name = "fcm_token", nullable = false, unique = true, length = 512)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private Platform platform;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "last_registered_at")
    private LocalDateTime lastRegisteredAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;
}
