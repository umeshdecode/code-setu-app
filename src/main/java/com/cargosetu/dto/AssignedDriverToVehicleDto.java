package com.cargosetu.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedDriverToVehicleDto {

    private Long id;

    // Driver summary
    private Long driverId;
    private String driverName; // Useful for UI display

    // Vehicle summary
    private Long vehicleId;
    private String registrationNumber; // Matches your frontend search logic

    private LocalDateTime assignedAt;
    private LocalDateTime unassignedAt;
    private Boolean active;
    private String remarks;

    // Audit fields (optional, usually good for admin views)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
