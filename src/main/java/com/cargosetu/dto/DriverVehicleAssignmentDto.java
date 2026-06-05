package com.cargosetu.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DriverVehicleAssignmentDto {
    Long id;

    Long driverId;
    Long vehicleId;

    LocalDateTime assignedAt;
    LocalDateTime unassignedAt;

    Boolean active; // current assignment

    String remarks;
}
