package com.cargosetu.maps.client;

import com.cargosetu.maps.config.GoogleMapsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleRoutesClientParseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesDistanceDurationPolylineAndLegs() throws Exception {
        String json = "{\"routes\":[{\"distanceMeters\":1490000,\"duration\":\"90000s\","
                + "\"polyline\":{\"encodedPolyline\":\"_p~iF~ps|U\"},"
                + "\"legs\":[{\"distanceMeters\":600000,\"duration\":\"36000s\"},"
                + "{\"distanceMeters\":890000,\"duration\":\"54000s\"}]}]}";
        ComputedRoute route = GoogleRoutesClient.parse(mapper.readTree(json)).orElseThrow();
        assertThat(route.distanceMeters()).isEqualTo(1_490_000L);
        assertThat(route.durationSeconds()).isEqualTo(90_000L);
        assertThat(route.encodedPolyline()).isEqualTo("_p~iF~ps|U");
        assertThat(route.legs()).hasSize(2);
    }

    @Test
    void emptyResponseMeansNoRoute() throws Exception {
        assertThat(GoogleRoutesClient.parse(mapper.readTree("{}"))).isEmpty();
    }

    @Test
    void mapsProviderStatuses() {
        assertThat(GoogleRoutesClient.mapStatus(429).getReason()).isEqualTo(MapsProviderException.Reason.RATE_LIMITED);
        assertThat(GoogleRoutesClient.mapStatus(403).getReason()).isEqualTo(MapsProviderException.Reason.ACCESS_DENIED);
        assertThat(GoogleRoutesClient.mapStatus(400).getReason()).isEqualTo(MapsProviderException.Reason.INVALID_REQUEST);
        assertThat(GoogleRoutesClient.mapStatus(503).isTransient()).isTrue();
    }

    @Test
    void requestUsesMinimalPlannedDriveOptions() {
        GoogleMapsProperties properties = new GoogleMapsProperties();
        var body = new GoogleRoutesClient(null, properties)
                .buildRequest(new LatLng(1, 1), new LatLng(2, 2), List.of());
        assertThat(body).containsEntry("travelMode", "DRIVE")
                .containsEntry("routingPreference", "TRAFFIC_UNAWARE")
                .containsEntry("computeAlternativeRoutes", false)
                .containsEntry("units", "METRIC")
                .doesNotContainKey("intermediates");
        assertThat(GoogleRoutesClient.FIELD_MASK).doesNotContain("*");
    }

    @Test
    void missingKeyFailsFastWithoutHttpCall() {
        GoogleRoutesClient client = new GoogleRoutesClient(null, new GoogleMapsProperties());
        assertThatThrownBy(() -> client.computeRoute(new LatLng(1, 1), new LatLng(2, 2), List.of()))
                .isInstanceOf(MapsProviderException.class)
                .extracting("reason").isEqualTo(MapsProviderException.Reason.NOT_CONFIGURED);
    }
}
