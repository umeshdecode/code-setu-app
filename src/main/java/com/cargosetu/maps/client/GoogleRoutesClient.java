package com.cargosetu.maps.client;

import com.cargosetu.maps.config.GoogleMapsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thin Google Routes API (computeRoutes) client. Requests a general DRIVE route,
 * traffic-unaware, no alternatives, metric units, and only the fields the UI uses.
 */
@Component
@Slf4j
public class GoogleRoutesClient {

    static final String FIELD_MASK = "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline";
    static final String FIELD_MASK_WITH_LEGS = FIELD_MASK + ",routes.legs.distanceMeters,routes.legs.duration";

    private final RestClient restClient;
    private final GoogleMapsProperties properties;

    public GoogleRoutesClient(@Qualifier("googleMapsRestClient") RestClient restClient,
                              GoogleMapsProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * @return the computed route, or empty when the provider returned no route
     * @throws MapsProviderException for configuration, quota, timeout or provider errors
     */
    public Optional<ComputedRoute> computeRoute(LatLng origin, LatLng destination, List<LatLng> intermediates) {
        if (!properties.isConfigured()) {
            throw new MapsProviderException(MapsProviderException.Reason.NOT_CONFIGURED, null,
                    "Google Maps server API key is not configured");
        }
        boolean withLegs = intermediates != null && !intermediates.isEmpty();
        Map<String, Object> body = buildRequest(origin, destination, intermediates);

        int attempt = 0;
        while (true) {
            long started = System.currentTimeMillis();
            try {
                JsonNode json = restClient.post()
                        .uri(properties.getRoutesUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Goog-Api-Key", properties.getServerApiKey())
                        .header("X-Goog-FieldMask", withLegs ? FIELD_MASK_WITH_LEGS : FIELD_MASK)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
                log.info("Routes API computeRoutes ok in {} ms (stops={})",
                        System.currentTimeMillis() - started, withLegs ? intermediates.size() : 0);
                return parse(json);
            } catch (RestClientResponseException e) {
                MapsProviderException mapped = mapStatus(e.getStatusCode().value());
                log.warn("Routes API computeRoutes failed: status={} reason={} in {} ms",
                        e.getStatusCode().value(), mapped.getReason(), System.currentTimeMillis() - started);
                if (!mapped.isTransient() || attempt >= properties.getMaxRetries()) {
                    throw mapped;
                }
            } catch (ResourceAccessException e) {
                log.warn("Routes API computeRoutes I/O failure ({}) in {} ms",
                        e.getClass().getSimpleName(), System.currentTimeMillis() - started);
                if (attempt >= properties.getMaxRetries()) {
                    throw new MapsProviderException(MapsProviderException.Reason.TIMEOUT, null,
                            "Routes API did not respond in time");
                }
            }
            attempt++;
            backoff(attempt);
        }
    }

    Map<String, Object> buildRequest(LatLng origin, LatLng destination, List<LatLng> intermediates) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("origin", waypoint(origin));
        body.put("destination", waypoint(destination));
        if (intermediates != null && !intermediates.isEmpty()) {
            body.put("intermediates", intermediates.stream().map(GoogleRoutesClient::waypoint).toList());
        }
        body.put("travelMode", "DRIVE");
        body.put("routingPreference", "TRAFFIC_UNAWARE");
        body.put("computeAlternativeRoutes", false);
        body.put("units", "METRIC");
        body.put("languageCode", properties.getLanguageCode());
        body.put("regionCode", properties.getRegionCode());
        return body;
    }

    private static Map<String, Object> waypoint(LatLng point) {
        return Map.of("location", Map.of("latLng",
                Map.of("latitude", point.latitude(), "longitude", point.longitude())));
    }

    static Optional<ComputedRoute> parse(JsonNode json) {
        if (json == null || !json.has("routes") || !json.get("routes").isArray() || json.get("routes").isEmpty()) {
            return Optional.empty();
        }
        JsonNode route = json.get("routes").get(0);
        String polyline = route.path("polyline").path("encodedPolyline").asText(null);
        if (polyline == null || polyline.isBlank()) {
            return Optional.empty();
        }
        List<ComputedRoute.Leg> legs = new ArrayList<>();
        for (JsonNode leg : route.path("legs")) {
            legs.add(new ComputedRoute.Leg(longOrNull(leg.get("distanceMeters")), parseDuration(leg.get("duration"))));
        }
        return Optional.of(new ComputedRoute(
                longOrNull(route.get("distanceMeters")),
                parseDuration(route.get("duration")),
                polyline,
                legs));
    }

    /** Routes API encodes durations as protobuf Duration strings, e.g. "5400s". */
    static Long parseDuration(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText("").trim();
        if (text.endsWith("s")) {
            text = text.substring(0, text.length() - 1);
        }
        try {
            return Math.round(Double.parseDouble(text));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long longOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asLong();
    }

    static MapsProviderException mapStatus(int status) {
        if (status == 429) {
            return new MapsProviderException(MapsProviderException.Reason.RATE_LIMITED, status, "Routes API quota or rate limit reached");
        }
        if (status == 401 || status == 403) {
            return new MapsProviderException(MapsProviderException.Reason.ACCESS_DENIED, status, "Routes API rejected the server credential or the API is not enabled");
        }
        if (status >= 400 && status < 500) {
            return new MapsProviderException(MapsProviderException.Reason.INVALID_REQUEST, status, "Routes API rejected the route request");
        }
        return new MapsProviderException(MapsProviderException.Reason.UNAVAILABLE, status, "Routes API is temporarily unavailable");
    }

    private static void backoff(int attempt) {
        try {
            Thread.sleep(300L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
