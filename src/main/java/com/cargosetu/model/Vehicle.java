package com.cargosetu.model;

import com.cargosetu.dto.VehicleDto;
import com.cargosetu.loadmatch.utils.VehicleTypeCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "vehicles")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Chassis number is mandatory")
    @Column(unique = true, nullable = false)
    private String chassisNumber;

    @NotBlank(message = "Engine number is mandatory")
    @Column(unique = true, nullable = false)
    private String engineNumber;

    @PositiveOrZero(message = "Mileage must be zero or positive")
    private Double mileage;

    @NotBlank(message = "Registration number is mandatory")
    @Size(min = 5, max = 20, message = "Registration number length invalid")
    @Column(unique = true, nullable = false)
    private String registrationNumber;

    @NotBlank(message = "Vehicle type is mandatory")
    @Pattern(regexp = VehicleTypeCategory.VALIDATION_PATTERN, message = "Invalid vehicle type")
    private String vehicleType; // e.g., OPEN, CONTAINER, LCV

    @NotBlank(message = "Model is mandatory")
    private String model;

    @NotBlank(message = "Manufacturer is mandatory")
    private String manufacturer;

    @NotNull(message = "Capacity is mandatory")
    @Min(value = 1, message = "Capacity must be at least 1kg")
    private Integer capacityKg;

    @Pattern(regexp = "AVAILABLE|ON_TRIP|MAINTENANCE|OUT_OF_SERVICE",
            message = "Invalid vehicle status")
    private String status;

    @NotNull(message = "Insurance expiry date is required")
    @Future(message = "Insurance must not be expired for new entries")
    private LocalDate insuranceExpiryDate;

    @NotNull(message = "Pollution expiry date is required")
    @Future(message = "Pollution certificate must not be expired")
    private LocalDate pollutionExpiryDate;

    @Pattern(regexp = "DIESEL|PETROL|ELECTRIC|CNG", message = "Invalid fuel type")
    private String fuelType;

    @PastOrPresent(message = "Registration date cannot be in the future")
    private LocalDate registrationDate;

    private String currentLocation;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Smart Load Matching - additional capacity/equipment fields
    private Double capacityCbm;

    private Double lengthCm;

    private Double widthCm;

    private Double heightCm;

    private String bodyType;

    private String equipmentTags;

    public Vehicle(VehicleDto dto) {
        if (dto == null) {
            return;
        }

        this.id = dto.getId();
        this.chassisNumber = dto.getChassisNumber();
        this.engineNumber = dto.getEngineNumber();
        this.mileage = dto.getMileage();
        this.registrationNumber = dto.getRegistrationNumber();
        this.vehicleType = dto.getVehicleType();
        this.model = dto.getModel();
        this.manufacturer = dto.getManufacturer();
        this.capacityKg = dto.getCapacityKg();
        this.status = dto.getStatus();
        this.insuranceExpiryDate = dto.getInsuranceExpiryDate();
        this.fuelType = dto.getFuelType();
        this.registrationDate = dto.getRegistrationDate();
        this.pollutionExpiryDate = dto.getPollutionExpiryDate();
        this.currentLocation = dto.getCurrentLocation();
        this.bodyType = dto.getBodyType();
        this.capacityCbm = dto.getCapacityCbm();
        this.lengthCm = dto.getLengthCm();
        this.widthCm = dto.getWidthCm();
        this.heightCm = dto.getHeightCm();
        this.equipmentTags = dto.getEquipmentTags();
    }

    public Vehicle() {

    }
}
