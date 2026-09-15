package com.cargosetu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * A staff member's own UI preferences, persisted as JSON in tblm_staff.theme_settings
 * so the chosen appearance follows them across browsers and devices.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPreferencesDTO {

    @Pattern(regexp = "^$|^(light|dark|system)$", message = "Mode must be light, dark or system")
    private String mode;

    @Size(max = 200, message = "Font family must be at most 200 characters")
    private String fontFamily;

    /** Accent colour picked from the fixed "Web light blue" palette on the frontend. */
    @Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Primary colour must be a 6-digit hex value")
    private String primaryColor;
}
