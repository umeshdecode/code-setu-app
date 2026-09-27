package com.cargosetu.maps.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Staff confirmation of one route point. Addresses are never overwritten. */
@Data
public class RouteLocationUpdateDTO {
    @NotNull
    private RouteEndpoint endpoint;

    /** Required when endpoint = STOP. */
    private Long tripStopId;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;

    /** GEOCODE_CONFIRMED, MAP_PIN or MANUAL. */
    @Pattern(regexp = "^(GEOCODE_CONFIRMED|MAP_PIN|MANUAL)$", message = "Invalid location source")
    private String locationSource = "GEOCODE_CONFIRMED";
}
