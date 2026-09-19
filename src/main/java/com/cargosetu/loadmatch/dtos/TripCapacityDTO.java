package com.cargosetu.loadmatch.dtos;

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
    private String vehicleTypeCategory;
    private String equipmentTags;
}
