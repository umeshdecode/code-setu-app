package com.cargosetu.dto;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PlanDTO {

    private Integer planId;

    @NotBlank(message = "Plan name is required")
    @Size(min = 3, max = 50)
    private String name;

    @NotNull(message = "Plan validity is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate planValidity; // Changed from LocalDateTime to LocalDate


    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    private Double price;

    @NotBlank(message = "Plan category is required")
    private String plannCategory;
}

