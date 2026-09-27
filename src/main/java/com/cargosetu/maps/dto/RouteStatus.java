package com.cargosetu.maps.dto;

public enum RouteStatus {
    /** Route computed; markers, polyline and metrics are available. */
    READY,
    /** Source and/or destination has no confirmed coordinate yet. */
    MISSING_LOCATION,
    /** The provider found no drivable route between the confirmed points. */
    NO_ROUTE,
    /** Quota / rate limit reached - retry later. */
    RATE_LIMITED,
    /** Timeout or provider-side failure - retry later. */
    PROVIDER_UNAVAILABLE,
    /** Server key missing or rejected (API not enabled / key restricted). */
    NOT_CONFIGURED
}
