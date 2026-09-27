package com.cargosetu.maps.client;

public record LatLng(double latitude, double longitude) {

    /** Stable key used for route caching (≈11 cm precision). */
    public String cacheKey() {
        return String.format(java.util.Locale.ROOT, "%.6f,%.6f", latitude, longitude);
    }
}
