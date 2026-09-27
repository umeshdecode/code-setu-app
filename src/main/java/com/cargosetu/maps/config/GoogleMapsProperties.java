package com.cargosetu.maps.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Server-side Google Maps Platform settings (Routes API + Geocoding API).
 *
 * The server key is only ever read from the environment (GOOGLE_MAPS_SERVER_API_KEY)
 * and is separate from the browser Maps JavaScript key in the Angular env.js.
 * It must never be logged, returned in a response, or committed.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.maps")
public class GoogleMapsProperties {

    private String serverApiKey = "";

    private String routesUrl = "https://routes.googleapis.com/directions/v2:computeRoutes";

    private String geocodeUrl = "https://maps.googleapis.com/maps/api/geocode/json";

    private int connectTimeoutMs = 3000;

    private int readTimeoutMs = 8000;

    /** Extra attempts for transient failures only (timeouts / 5xx). Never for 4xx. */
    private int maxRetries = 1;

    /**
     * In-memory reuse window for an identical set of route coordinates. 0 disables it.
     * Review Google Maps Platform service terms before raising this value.
     */
    private long routeCacheTtlSeconds = 900;

    private int routeCacheMaxEntries = 500;

    private String regionCode = "IN";

    private String languageCode = "en-IN";

    /** Routes API accepts at most 25 intermediate waypoints. */
    private int maxIntermediateStops = 25;

    public boolean isConfigured() {
        return serverApiKey != null && !serverApiKey.isBlank();
    }
}
