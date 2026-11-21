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

    // Load Posting & Vehicle Classification Options
    private String truckType; // e.g., OPEN_TRUCK, CONTAINER, LCV, TIPPER, TANKER, TRAILER
    private String bodyType; // e.g., LCV_OPEN, LCV_CONTAINER, OPEN_FULL_BODY, CLOSED_CONTAINER
    private Double truckLengthFeet; // e.g., 17.0, 19.0, 32.0
    private String lengthOption; // e.g., "17 Feet", "19 Feet", "32 Feet SXL"
    private Integer wheelerCount; // e.g., 4, 6, 14, 18
    private String wheelerOption; // e.g., "4 Wheeler", "6 Wheeler", "14 Wheeler"
    private Double passingCapacityTons; // e.g., 24.5, 15.0
    private String passingCapacityOption; // e.g., "24-25 Ton", "15 Ton"
}
