package com.cargosetu.maps.client;

import com.cargosetu.maps.config.GoogleMapsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Geocoding API client used only to *suggest* candidate points for an
 * address. Staff must pick/confirm a candidate; nothing is saved automatically.
 * The key travels as a query parameter here, so URLs/exception messages from this
 * client are never logged.
 */
@Component
@Slf4j
public class GoogleGeocodingClient {

    private static final int MAX_CANDIDATES = 5;

    private final RestClient restClient;
    private final GoogleMapsProperties properties;

    public GoogleGeocodingClient(@Qualifier("googleMapsRestClient") RestClient restClient,
                                 GoogleMapsProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public record Candidate(String formattedAddress,
                            double latitude,
                            double longitude,
                            String locationType,
                            List<String> types,
                            boolean partialMatch,
                            String placeId) {
    }

    public List<Candidate> geocode(String address) {
        if (!properties.isConfigured()) {
            throw new MapsProviderException(MapsProviderException.Reason.NOT_CONFIGURED, null,
                    "Google Maps server API key is not configured");
        }
        long started = System.currentTimeMillis();
        try {
            String uri = UriComponentsBuilder.fromUriString(properties.getGeocodeUrl())
                    .queryParam("address", address)
                    .queryParam("region", properties.getRegionCode().toLowerCase())
                    .queryParam("language", properties.getLanguageCode())
                    .queryParam("key", properties.getServerApiKey())
                    .encode()
                    .toUriString();
            JsonNode json = restClient.get().uri(java.net.URI.create(uri)).retrieve().body(JsonNode.class);
            String status = json == null ? "UNKNOWN_ERROR" : json.path("status").asText("UNKNOWN_ERROR");
            log.info("Geocoding API status={} in {} ms", status, System.currentTimeMillis() - started);
            return switch (status) {
                case "OK" -> parse(json);
                case "ZERO_RESULTS" -> List.of();
                case "OVER_QUERY_LIMIT", "OVER_DAILY_LIMIT" -> throw new MapsProviderException(
                        MapsProviderException.Reason.RATE_LIMITED, null, "Geocoding API quota or rate limit reached");
                case "REQUEST_DENIED" -> throw new MapsProviderException(
                        MapsProviderException.Reason.ACCESS_DENIED, null, "Geocoding API rejected the server credential or the API is not enabled");
                case "INVALID_REQUEST" -> throw new MapsProviderException(
                        MapsProviderException.Reason.INVALID_REQUEST, null, "Geocoding API rejected the address query");
                default -> throw new MapsProviderException(
                        MapsProviderException.Reason.UNAVAILABLE, null, "Geocoding API is temporarily unavailable");
            };
        } catch (RestClientResponseException e) {
            log.warn("Geocoding API HTTP failure status={} in {} ms", e.getStatusCode().value(), System.currentTimeMillis() - started);
            throw GoogleRoutesClient.mapStatus(e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            log.warn("Geocoding API I/O failure ({}) in {} ms", e.getClass().getSimpleName(), System.currentTimeMillis() - started);
            throw new MapsProviderException(MapsProviderException.Reason.TIMEOUT, null, "Geocoding API did not respond in time");
        }
    }

    static List<Candidate> parse(JsonNode json) {
        List<Candidate> candidates = new ArrayList<>();
        for (JsonNode result : json.path("results")) {
            JsonNode location = result.path("geometry").path("location");
            if (!location.has("lat") || !location.has("lng")) {
                continue;
            }
            List<String> types = new ArrayList<>();
            result.path("types").forEach(t -> types.add(t.asText()));
            candidates.add(new Candidate(
                    result.path("formatted_address").asText(null),
                    location.get("lat").asDouble(),
                    location.get("lng").asDouble(),
                    result.path("geometry").path("location_type").asText(null),
                    types,
                    result.path("partial_match").asBoolean(false),
                    result.path("place_id").asText(null)));
            if (candidates.size() >= MAX_CANDIDATES) {
                break;
            }
        }
        return candidates;
    }
}
