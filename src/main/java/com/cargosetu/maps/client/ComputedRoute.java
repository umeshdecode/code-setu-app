package com.cargosetu.maps.client;

import java.util.List;

/** Normalized result of a Routes API computeRoutes call. */
public record ComputedRoute(Long distanceMeters,
                            Long durationSeconds,
                            String encodedPolyline,
                            List<Leg> legs) {

    public record Leg(Long distanceMeters, Long durationSeconds) {
    }
}
