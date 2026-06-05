package com.cargosetu.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
public class VehicleDto {
    Long id;
    private String chassisNumber;
    private String engineNumber;
    Double mileage;
    private String registrationNumber;
    private String vehicleType;
    private String model;
    private String manufacturer;
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
