package com.cargosetu.maps;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.customer.entity.Shifting;
import com.cargosetu.loadmatch.entity.TripStop;
import com.cargosetu.loadmatch.repository.TripStopRepository;
import com.cargosetu.maps.client.ComputedRoute;
import com.cargosetu.maps.client.GoogleGeocodingClient;
import com.cargosetu.maps.client.GoogleRoutesClient;
import com.cargosetu.maps.client.MapsProviderException;
import com.cargosetu.maps.config.GoogleMapsProperties;
import com.cargosetu.maps.dto.*;
import com.cargosetu.maps.service.RouteCache;
import com.cargosetu.maps.service.TripRouteService;
import com.cargosetu.model.Trip;
import com.cargosetu.model.TripStatus;
import com.cargosetu.repo.ShiftingRepository;
import com.cargosetu.repo.TripRepository;
import com.cargosetu.service.TripAccessPolicy;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Covers the implementation plan's route tests: valid route (T01), missing/ambiguous
 * location (T02/T03), no route (T04), provider timeout (T05), rejected key (T06),
 * cross-company access (T07), endpoint change invalidation (T08), ordered stops (T09)
 * and repeated requests (T13).
 */
@ExtendWith(MockitoExtension.class)
class TripRouteServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private ShiftingRepository shiftingRepository;
    @Mock private TripStopRepository tripStopRepository;
    @Mock private GoogleRoutesClient routesClient;
    @Mock private GoogleGeocodingClient geocodingClient;

    private TripRouteService service;
    private LoggedInUser user;

    @BeforeEach
    void setUp() {
        GoogleMapsProperties properties = new GoogleMapsProperties();
        properties.setServerApiKey("test-key");
        service = new TripRouteService(tripRepository, shiftingRepository, tripStopRepository,
                new TripAccessPolicy(), routesClient, geocodingClient, new RouteCache(properties), properties);
        user = new LoggedInUser();
        user.setStaffId(42L);
        user.setCompanyId(7);
        user.setEmail("staff@example.com");
        lenient().when(tripStopRepository.findByTrip_TripIdOrderBySequenceOrderAsc(anyLong())).thenReturn(List.of());
    }

    private Trip trip(Integer companyId, Double fromLat, Double fromLng, Double toLat, Double toLng) {
        Shifting shifting = new Shifting();
        shifting.setShiftFromCity("Ahmedabad");
        shifting.setShiftToCity("Bangalore");
        shifting.setFromLatitude(fromLat);
        shifting.setFromLongitude(fromLng);
        shifting.setToLatitude(toLat);
        shifting.setToLongitude(toLng);
        Trip trip = new Trip();
        trip.setTripId(10L);
        trip.setTripNumber("TRP-10");
        trip.setStatus(TripStatus.CREATED);
        trip.setCompanyId(companyId);
        trip.setShifting(shifting);
        return trip;
    }

    private ComputedRoute sampleRoute() {
        return new ComputedRoute(1_490_000L, 90_000L, "abc", List.of());
    }

    @Test
    void returnsNormalizedRouteForConfirmedTrip() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(7, 23.02, 72.57, 12.97, 77.59)));
        when(routesClient.computeRoute(any(), any(), anyList())).thenReturn(Optional.of(sampleRoute()));

        GenericDataDTO response = service.getTripRoute(user, 10L, false);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.SUCCESS);
        TripRouteDTO route = (TripRouteDTO) response.getData();
        assertThat(route.getRouteStatus()).isEqualTo(RouteStatus.READY);
        assertThat(route.getDistanceMeters()).isEqualTo(1_490_000L);
        assertThat(route.getDurationSeconds()).isEqualTo(90_000L);
        assertThat(route.getEncodedPolyline()).isEqualTo("abc");
        assertThat(route.getRouteType()).isEqualTo("PLANNED_DRIVING");
        assertThat(route.getSource().isConfirmed()).isTrue();
    }

    @Test
    void missingCoordinatesNeverCallProviderOrInventPoints() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(7, null, null, 12.97, 77.59)));

        GenericDataDTO response = service.getTripRoute(user, 10L, false);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.VALIDATION_ERROR);
        TripRouteDTO route = (TripRouteDTO) response.getData();
        assertThat(route.getRouteStatus()).isEqualTo(RouteStatus.MISSING_LOCATION);
        assertThat(route.getMissingEndpoints()).containsExactly(RouteEndpoint.SOURCE);
        assertThat(route.getSource().getLatitude()).isNull();
        assertThat(route.getSource().getCity()).isEqualTo("Ahmedabad");
        verifyNoInteractions(routesClient);
    }

    @Test
    void noRouteKeepsTripDetails() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(7, 23.02, 72.57, 12.97, 77.59)));
        when(routesClient.computeRoute(any(), any(), anyList())).thenReturn(Optional.empty());

        GenericDataDTO response = service.getTripRoute(user, 10L, false);

        TripRouteDTO route = (TripRouteDTO) response.getData();
        assertThat(route.getRouteStatus()).isEqualTo(RouteStatus.NO_ROUTE);
        assertThat(route.getTripNumber()).isEqualTo("TRP-10");
        assertThat(route.getEncodedPolyline()).isNull();
    }

    @Test
    void providerTimeoutIsSafeTemporaryUnavailable() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(7, 23.02, 72.57, 12.97, 77.59)));
        when(routesClient.computeRoute(any(), any(), anyList()))
                .thenThrow(new MapsProviderException(MapsProviderException.Reason.TIMEOUT, null, "timeout"));

        GenericDataDTO response = service.getTripRoute(user, 10L, false);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.SERVICE_UNAVAILABLE);
        assertThat(((TripRouteDTO) response.getData()).getRouteStatus()).isEqualTo(RouteStatus.PROVIDER_UNAVAILABLE);
    }

    @Test
    void rejectedKeyDoesNotLeakProviderDetails() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(7, 23.02, 72.57, 12.97, 77.59)));
        when(routesClient.computeRoute(any(), any(), anyList()))
                .thenThrow(new MapsProviderException(MapsProviderException.Reason.ACCESS_DENIED, 403, "denied"));

        GenericDataDTO response = service.getTripRoute(user, 10L, false);

        TripRouteDTO route = (TripRouteDTO) response.getData();
        assertThat(route.getRouteStatus()).isEqualTo(RouteStatus.NOT_CONFIGURED);
        assertThat(response.getResponseMessege()).doesNotContain("test-key").doesNotContain("403");
    }

    @Test
    void crossCompanyTripIsNotFound() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(99, 23.02, 72.57, 12.97, 77.59)));

        GenericDataDTO response = service.getTripRoute(user, 10L, false);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.NOT_FOUND);
        assertThat(response.getData()).isNull();
        verifyNoInteractions(routesClient);
    }

    @Test
    void repeatedRequestReusesRouteButChangedEndpointRecalculates() {
        Trip trip = trip(7, 23.02, 72.57, 12.97, 77.59);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(routesClient.computeRoute(any(), any(), anyList())).thenReturn(Optional.of(sampleRoute()));

        service.getTripRoute(user, 10L, false);
        GenericDataDTO second = service.getTripRoute(user, 10L, false);
        assertThat(((TripRouteDTO) second.getData()).getCached()).isTrue();
        verify(routesClient, times(1)).computeRoute(any(), any(), anyList());

        trip.getShifting().setToLatitude(13.05);
        GenericDataDTO third = service.getTripRoute(user, 10L, false);
        assertThat(((TripRouteDTO) third.getData()).getCached()).isFalse();
        verify(routesClient, times(2)).computeRoute(any(), any(), anyList());
    }

    @Test
    void orderedConfirmedStopsAreRoutedAndUnconfirmedAreSkipped() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(7, 23.02, 72.57, 12.97, 77.59)));
        TripStop confirmed = new TripStop();
        confirmed.setTripStopId(1L);
        confirmed.setSequenceOrder(1);
        confirmed.setStopCity("Pune");
        confirmed.setLatitude(18.52);
        confirmed.setLongitude(73.85);
        TripStop unconfirmed = new TripStop();
        unconfirmed.setTripStopId(2L);
        unconfirmed.setSequenceOrder(2);
        unconfirmed.setStopCity("Hubli");
        when(tripStopRepository.findByTrip_TripIdOrderBySequenceOrderAsc(10L)).thenReturn(List.of(confirmed, unconfirmed));
        when(routesClient.computeRoute(any(), any(), anyList())).thenReturn(Optional.of(sampleRoute()));

        GenericDataDTO response = service.getTripRoute(user, 10L, false);

        TripRouteDTO route = (TripRouteDTO) response.getData();
        assertThat(route.getStops()).extracting(RouteStopDTO::isIncludedInRoute).containsExactly(true, false);
        assertThat(route.getStopsSkipped()).isEqualTo(1);
        verify(routesClient).computeRoute(any(), any(),
                argThat(list -> list.size() == 1 && list.get(0).latitude() == 18.52));
    }

    @Test
    void confirmSourceStoresCoordinateWithoutTouchingAddress() {
        Trip trip = trip(7, null, null, 12.97, 77.59);
        trip.getShifting().setFromAddress("Plot 12, GIDC");
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        RouteLocationUpdateDTO update = new RouteLocationUpdateDTO();
        update.setEndpoint(RouteEndpoint.SOURCE);
        update.setLatitude(23.0225);
        update.setLongitude(72.5714);

        GenericDataDTO response = service.confirmLocation(user, 10L, update);

        assertThat(response.getResponseCode()).isEqualTo(CommonStatusCode.SUCCESS);
        assertThat(trip.getShifting().getFromLatitude()).isEqualTo(23.0225);
        assertThat(trip.getShifting().getFromAddress()).isEqualTo("Plot 12, GIDC");
        assertThat(trip.getShifting().getFromLocationConfirmedBy()).isEqualTo("staff@example.com");
        verify(shiftingRepository).save(trip.getShifting());
    }

    @Test
    void cityLevelGeocodeCandidateIsFlaggedApproximate() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(7, null, null, null, null)));
        when(geocodingClient.geocode("Ahmedabad")).thenReturn(List.of(
                new GoogleGeocodingClient.Candidate("Ahmedabad, Gujarat, India", 23.02, 72.57,
                        "APPROXIMATE", List.of("locality", "political"), false, "p1")));
        GeocodeRequestDTO request = new GeocodeRequestDTO();
        request.setQuery("Ahmedabad");

        GenericDataDTO response = service.geocode(user, 10L, request);

        @SuppressWarnings("unchecked")
        List<GeocodeCandidateDTO> candidates = (List<GeocodeCandidateDTO>) response.getData();
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).isApproximate()).isTrue();
        verifyNoInteractions(shiftingRepository);
    }
}
