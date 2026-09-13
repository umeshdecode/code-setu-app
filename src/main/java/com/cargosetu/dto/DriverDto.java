package com.cargosetu.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DriverDto {
    private Long id;


    private String name;

    private String phoneNumber;

    private String email;


    private String licenseNumber; // UNIQUE

    private LocalDate licenseExpiryDate;


    @NotNull(message = "Experience years is required")
    @PositiveOrZero(message = "Experience years cannot be negative")
    private Integer experienceYears;


    private String address;


    private String status; // ACTIVE, INACTIVE, SUSPENDED


    private LocalDate joiningDate;


    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
