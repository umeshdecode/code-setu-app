package com.cargosetu.dto;

import com.cargosetu.loadmatch.utils.VehicleTypeCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @Pattern(regexp = VehicleTypeCategory.VALIDATION_PATTERN, message = "Invalid vehicle type")
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

    // Smart Load Matching - additional capacity/equipment fields (bug fix:
    // these existed on the Vehicle entity but were never bound from incoming
    // requests because the controller binds to VehicleDto, not Vehicle).
    private String bodyType;
    private Double capacityCbm;
    private Double lengthCm;
    private Double widthCm;
    private Double heightCm;
    private String equipmentTags;

}
