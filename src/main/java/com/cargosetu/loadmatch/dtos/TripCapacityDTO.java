package com.cargosetu.loadmatch.dtos;

import com.cargosetu.loadmatch.utils.VehicleTypeCategory;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TripCapacityDTO {
    private Long tripId;
    private Double remainingWeightKg;
    private Double remainingVolumeCbm;
    private Double totalWeightKg;
    private Double totalVolumeCbm;
    private Double vehicleCapacityKg;
    private Double vehicleCapacityCbm;

    @Pattern(regexp = "^$|" + VehicleTypeCategory.VALIDATION_PATTERN, message = "Invalid vehicle type category")
    private String vehicleTypeCategory;
    private String equipmentTags;
}
