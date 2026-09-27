package com.cargosetu.maps.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteStopDTO {
    private Long tripStopId;
    private Integer sequenceOrder;
    private String city;
    private Double latitude;
    private Double longitude;
    private boolean confirmed;
    /** False when the stop has no coordinate and was therefore left out of the route. */
    private boolean includedInRoute;
}
