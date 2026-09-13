package com.cargosetu.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SelectPlanRequestDTO {

    @NotNull(message = "Plan is required")
    private Integer planId;
}
