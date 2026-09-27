package com.cargosetu.dto;

import com.cargosetu.model.TripStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TripDTO {
    private Long tripId;
    private String tripNumber;
    private Long shiftingId;
    private Long driverId;
    private Long vehicleId;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private Double plannedDistanceKm;
    private Double actualDistanceKm;
    private TripStatus status;
    private String remarks;
    private Boolean active;

    // Read-only, joined from the linked Shifting (ignored on create/update).
    private String sourceCity;
    private String destinationCity;
}
