package com.cargosetu.push.dto;

import com.cargosetu.push.model.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PushTokenRegisterDTO {

    @NotBlank
    @Size(max = 190)
    private String deviceId;

    @NotBlank
    @Size(max = 512)
    private String fcmToken;

    @NotNull
    private Platform platform;

    @Size(max = 190)
    private String deviceName;
}
