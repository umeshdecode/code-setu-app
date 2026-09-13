package com.cargosetu.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
public class VehicleDto {
    Long id;
    private String chassisNumber;
    private String engineNumber;

    @NotNull(message = "Mileage is required")
    @PositiveOrZero(message = "Mileage cannot be negative")
    Double mileage;

    private String registrationNumber;
    private String vehicleType;
    private String model;
    private String manufacturer;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be greater than 0")
    private Integer capacityKg;
    private String status;
    private LocalDate insuranceExpiryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String fuelType; // DIESEL, PETROL, ELECTRIC
    private LocalDate registrationDate;
    LocalDate pollutionExpiryDate;
    private String currentLocation; // Optional (can normalize later)

}
