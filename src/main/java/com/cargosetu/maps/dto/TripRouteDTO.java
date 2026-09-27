package com.cargosetu.maps.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Normalized planned-route payload for the Trip Details map. Optional values are
 * omitted rather than fabricated when they are not available.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TripRouteDTO {

    public static final String ROUTE_TYPE_PLANNED_DRIVING = "PLANNED_DRIVING";
    public static final String DISCLAIMER = "Planned general driving route. Distance and time are estimates, "
            + "not live vehicle tracking and not verified for truck size/weight restrictions.";

    private Long tripId;
    private String tripNumber;
    private String tripStatus;
    private RouteStatus routeStatus;
    private String message;

    private RoutePointDTO source;
    private RoutePointDTO destination;
    private List<RouteStopDTO> stops = new ArrayList<>();
    /** SOURCE / DESTINATION entries that still need a confirmed coordinate. */
    private List<RouteEndpoint> missingEndpoints = new ArrayList<>();
    private Integer stopsSkipped;

    private Long distanceMeters;
    private Long durationSeconds;
    private String encodedPolyline;
    private List<RouteLegDTO> legs;
    private Double plannedDistanceKm;

    private String routeType = ROUTE_TYPE_PLANNED_DRIVING;
    private String disclaimer = DISCLAIMER;
    private Instant calculatedAt;
    private Boolean cached;
    /** Request correlation id for support; never contains provider data. */
    private String referenceId;
}
