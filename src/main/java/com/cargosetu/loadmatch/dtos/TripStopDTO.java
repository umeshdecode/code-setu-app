package com.cargosetu.loadmatch.dtos;

import lombok.Data;

@Data
public class TripStopDTO {
    private Long tripStopId;
    private Long tripId;
    private String stopCity;
    private Integer sequenceOrder;
}
