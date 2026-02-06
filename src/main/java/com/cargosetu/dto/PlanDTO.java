package com.cargosetu.dto;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlanDTO {

    private Integer planId;

    @NotBlank(message = "Plan name is required")
    @Size(min = 3, max = 50)
    private String planName;

    @NotNull(message = "Plan validity is required")
    private LocalDateTime planValidity;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    private Double price;

    @NotBlank(message = "Plan category is required")
    private String planCategory;
}

