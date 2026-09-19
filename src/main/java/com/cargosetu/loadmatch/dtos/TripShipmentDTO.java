package com.cargosetu.loadmatch.dtos;

import com.cargosetu.loadmatch.entity.ManifestStatus;
import lombok.Data;

@Data
public class TripShipmentDTO {
    private Long tripShipmentId;
    private Long tripId;
    private Long shipmentId;
    private Long loadMatchId;

    private Double allocatedWeightKg;
    private Double allocatedVolumeCbm;
    private Integer sequenceOrder;
    private ManifestStatus manifestStatus;

    // Flat summary fields so a manifest table row can render without a second API call.
    private String shipmentSourceCity;
    private String shipmentDestinationCity;
    private String shipmentCargoType;
}
