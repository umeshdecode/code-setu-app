package com.cargosetu.push.dto;

import com.cargosetu.push.model.Platform;
import lombok.Data;

import java.time.LocalDateTime;

/** Masked view for GET /push-tokens - the raw fcmToken is never returned. */
@Data
public class PushTokenDTO {
    private Long id;
    private Platform platform;
    private String deviceName;
    private LocalDateTime lastSeenAt;
    private LocalDateTime lastRegisteredAt;
}
