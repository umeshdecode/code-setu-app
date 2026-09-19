package com.cargosetu.loadmatch.dtos;

import com.cargosetu.loadmatch.entity.LoadMatchStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoadMatchDTO {
    private Long loadMatchId;
    private Long shipmentId;
    private Long tripId;

    // Flat summary fields so a UI list row can render without a second API call.
    private String shipmentSourceCity;
    private String shipmentDestinationCity;
    private String shipmentCargoType;
    private String tripSourceCity;
    private String tripDestinationCity;
    private String tripVehicleRegistrationNumber;

    private Double overallScore;
    private Double routeScore;
    private Double capacityScore;
    private Double weightScore;
    private Double timingScore;
    private Double vehicleScore;
    private Double handlingScore;

    private LoadMatchStatus status;
    private LocalDateTime expiresAt;
}
