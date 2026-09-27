package com.cargosetu.maps.service;

import com.cargosetu.config.RequestCorrelationFilter;
import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.customer.entity.Shifting;
import com.cargosetu.loadmatch.entity.TripStop;
import com.cargosetu.loadmatch.repository.TripStopRepository;
import com.cargosetu.maps.client.ComputedRoute;
import com.cargosetu.maps.client.GoogleGeocodingClient;
import com.cargosetu.maps.client.GoogleRoutesClient;
import com.cargosetu.maps.client.LatLng;
import com.cargosetu.maps.client.MapsProviderException;
import com.cargosetu.maps.config.GoogleMapsProperties;
import com.cargosetu.maps.dto.*;
import com.cargosetu.model.Trip;
import com.cargosetu.repo.ShiftingRepository;
import com.cargosetu.repo.TripRepository;
import com.cargosetu.service.TripAccessPolicy;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Planned driving route for a Trip (Google Routes API), plus the staff-driven
 * location confirmation flow (Geocoding suggestions + explicit confirm).
 *
 * Read-only for route data: nothing from the provider is persisted. Only
 * staff-confirmed coordinates are saved, on the Shifting (origin/destination)
 * and TripStop rows.
 */
@Service
@Slf4j
public class TripRouteService {

    private static final Set<String> CITY_LEVEL_TYPES = Set.of(
            "locality", "administrative_area_level_1", "administrative_area_level_2",
            "administrative_area_level_3", "country", "postal_code", "colloquial_area");

    private final TripRepository tripRepository;
    private final ShiftingRepository shiftingRepository;
    private final TripStopRepository tripStopRepository;
    private final TripAccessPolicy tripAccessPolicy;
    private final GoogleRoutesClient routesClient;
    private final GoogleGeocodingClient geocodingClient;
    private final RouteCache routeCache;
    private final GoogleMapsProperties properties;

    public TripRouteService(TripRepository tripRepository,
                            ShiftingRepository shiftingRepository,
                            TripStopRepository tripStopRepository,
                            TripAccessPolicy tripAccessPolicy,
                            GoogleRoutesClient routesClient,
                            GoogleGeocodingClient geocodingClient,
                            RouteCache routeCache,
                            GoogleMapsProperties properties) {
        this.tripRepository = tripRepository;
        this.shiftingRepository = shiftingRepository;
        this.tripStopRepository = tripStopRepository;
        this.tripAccessPolicy = tripAccessPolicy;
        this.routesClient = routesClient;
        this.geocodingClient = geocodingClient;
        this.routeCache = routeCache;
        this.properties = properties;
    }

    // ── Route ─────────────────────────────────────────────────────────────

    // Deliberately not @Transactional: the provider call must not hold a DB connection.
    public GenericDataDTO getTripRoute(LoggedInUser user, Long tripId, boolean refresh) {
        GenericDataDTO response = new GenericDataDTO();
        Optional<Trip> tripOptional = findAuthorizedTrip(user, tripId);
        if (tripOptional.isEmpty()) {
            return notFound(response, tripId);
        }
        Trip trip = tripOptional.get();
        TripRouteDTO route = describeTrip(trip);

        if (!route.getMissingEndpoints().isEmpty()) {
            route.setRouteStatus(RouteStatus.MISSING_LOCATION);
            route.setMessage(missingMessage(route.getMissingEndpoints()));
            return respond(response, CommonStatusCode.VALIDATION_ERROR, route);
        }

        LatLng origin = new LatLng(route.getSource().getLatitude(), route.getSource().getLongitude());
        LatLng destination = new LatLng(route.getDestination().getLatitude(), route.getDestination().getLongitude());
        List<LatLng> intermediates = route.getStops().stream()
                .filter(RouteStopDTO::isIncludedInRoute)
                .map(s -> new LatLng(s.getLatitude(), s.getLongitude()))
                .toList();
        String cacheKey = cacheKey(origin, destination, intermediates);

        try {
            Optional<RouteCache.Entry> cached = refresh ? Optional.empty() : routeCache.get(cacheKey);
            RouteCache.Entry entry;
            if (cached.isPresent()) {
                entry = cached.get();
                route.setCached(true);
            } else {
                Optional<ComputedRoute> computed = routesClient.computeRoute(origin, destination, intermediates);
                if (computed.isEmpty()) {
                    log.info("No route returned for trip {}", tripId);
                    route.setRouteStatus(RouteStatus.NO_ROUTE);
                    route.setMessage("A route could not be calculated for these locations.");
                    return respond(response, CommonStatusCode.VALIDATION_ERROR, route);
                }
                entry = routeCache.put(cacheKey, computed.get());
                route.setCached(false);
            }
            applyRoute(route, entry);
            return respond(response, CommonStatusCode.SUCCESS, route);
        } catch (MapsProviderException e) {
            log.warn("Route unavailable for trip {}: reason={} providerStatus={} ({})",
                    tripId, e.getReason(), e.getProviderStatus(), e.getMessage());
            route.setRouteStatus(toRouteStatus(e));
            route.setMessage(userMessage(e));
            route.setReferenceId(RequestCorrelationFilter.currentRequestId());
            return respond(response, CommonStatusCode.SERVICE_UNAVAILABLE, route);
        }
    }

    // ── Location confirmation ─────────────────────────────────────────────

    /** Returns up to 5 candidate points for staff to choose from. Nothing is saved. */
    public GenericDataDTO geocode(LoggedInUser user, Long tripId, GeocodeRequestDTO request) {
        GenericDataDTO response = new GenericDataDTO();
        if (findAuthorizedTrip(user, tripId).isEmpty()) {
            return notFound(response, tripId);
        }
        try {
            List<GeocodeCandidateDTO> candidates = geocodingClient.geocode(request.getQuery().trim()).stream()
                    .map(TripRouteService::toCandidate)
                    .toList();
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege(candidates.isEmpty()
                    ? "No matching location found. Try a more specific address or place the pin on the map."
                    : "Select and confirm the correct location.");
            response.setData(candidates);
        } catch (MapsProviderException e) {
            log.warn("Geocoding unavailable for trip {}: reason={} ({})", tripId, e.getReason(), e.getMessage());
            response.setResponseCode(CommonStatusCode.SERVICE_UNAVAILABLE);
            response.setResponseMessege(userMessage(e));
            response.setErrors(new Object[]{"referenceId=" + RequestCorrelationFilter.currentRequestId()});
        }
        return response;
    }

    @Transactional
    public GenericDataDTO confirmLocation(LoggedInUser user, Long tripId, RouteLocationUpdateDTO update) {
        GenericDataDTO response = new GenericDataDTO();
        Optional<Trip> tripOptional = findAuthorizedTrip(user, tripId);
        if (tripOptional.isEmpty()) {
            return notFound(response, tripId);
        }
        Trip trip = tripOptional.get();
        String confirmedBy = user != null ? user.getUsername() : null;
        String source = update.getLocationSource() != null ? update.getLocationSource() : "GEOCODE_CONFIRMED";

        switch (update.getEndpoint()) {
            case SOURCE, DESTINATION -> {
                Shifting shifting = trip.getShifting();
                if (shifting == null) {
                    response.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
                    response.setResponseMessege("This trip has no linked shifting to store the location on.");
                    return response;
                }
                if (update.getEndpoint() == RouteEndpoint.SOURCE) {
                    shifting.setFromLatitude(update.getLatitude());
                    shifting.setFromLongitude(update.getLongitude());
                    shifting.setFromLocationSource(source);
                    shifting.setFromLocationConfirmedAt(LocalDateTime.now());
                    shifting.setFromLocationConfirmedBy(confirmedBy);
                } else {
                    shifting.setToLatitude(update.getLatitude());
                    shifting.setToLongitude(update.getLongitude());
                    shifting.setToLocationSource(source);
                    shifting.setToLocationConfirmedAt(LocalDateTime.now());
                    shifting.setToLocationConfirmedBy(confirmedBy);
                }
                shiftingRepository.save(shifting);
            }
            case STOP -> {
                if (update.getTripStopId() == null) {
                    response.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
                    response.setResponseMessege("tripStopId is required for a stop location.");
                    return response;
                }
                Optional<TripStop> stop = tripStopRepository.findById(update.getTripStopId())
                        .filter(s -> s.getTrip() != null && tripId.equals(s.getTrip().getTripId()));
                if (stop.isEmpty()) {
                    response.setResponseCode(CommonStatusCode.NOT_FOUND);
                    response.setResponseMessege("Trip stop not found with ID: " + update.getTripStopId() + " for trip: " + tripId);
                    return response;
                }
                stop.get().setLatitude(update.getLatitude());
                stop.get().setLongitude(update.getLongitude());
                stop.get().setLocationSource(source);
                tripStopRepository.save(stop.get());
            }
        }
        log.info("Trip {} {} location confirmed by {} (source={})", tripId, update.getEndpoint(), confirmedBy, source);

        TripRouteDTO route = describeTrip(trip);
        route.setRouteStatus(route.getMissingEndpoints().isEmpty() ? null : RouteStatus.MISSING_LOCATION);
        response.setResponseCode(CommonStatusCode.SUCCESS);
        response.setResponseMessege("Location confirmed successfully!");
        response.setData(route);
        return response;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Optional<Trip> findAuthorizedTrip(LoggedInUser user, Long tripId) {
        Optional<Trip> trip = tripRepository.findById(tripId);
        if (trip.isPresent() && !tripAccessPolicy.canAccess(user, trip.get())) {
            log.warn("Trip {} route access denied for staff {}", tripId, user != null ? user.getStaffId() : null);
            return Optional.empty();
        }
        return trip;
    }

    TripRouteDTO describeTrip(Trip trip) {
        TripRouteDTO route = new TripRouteDTO();
        route.setTripId(trip.getTripId());
        route.setTripNumber(trip.getTripNumber());
        route.setTripStatus(trip.getStatus() != null ? trip.getStatus().name() : null);
        route.setPlannedDistanceKm(trip.getPlannedDistanceKm());

        Shifting shifting = trip.getShifting();
        RoutePointDTO source = new RoutePointDTO();
        RoutePointDTO destination = new RoutePointDTO();
        if (shifting != null) {
            source.setCity(shifting.getShiftFromCity());
            source.setAddress(shifting.getFromAddress());
            source.setLatitude(shifting.getFromLatitude());
            source.setLongitude(shifting.getFromLongitude());
            source.setLocationSource(shifting.getFromLocationSource());
            source.setConfirmedAt(shifting.getFromLocationConfirmedAt());
            source.setConfirmedBy(shifting.getFromLocationConfirmedBy());

            destination.setCity(shifting.getShiftToCity());
            destination.setAddress(shifting.getToAddress());
            destination.setLatitude(shifting.getToLatitude());
            destination.setLongitude(shifting.getToLongitude());
            destination.setLocationSource(shifting.getToLocationSource());
            destination.setConfirmedAt(shifting.getToLocationConfirmedAt());
            destination.setConfirmedBy(shifting.getToLocationConfirmedBy());
        }
        source.setConfirmed(hasPoint(source.getLatitude(), source.getLongitude()));
        destination.setConfirmed(hasPoint(destination.getLatitude(), destination.getLongitude()));
        route.setSource(source);
        route.setDestination(destination);
        if (!source.isConfirmed()) {
            route.getMissingEndpoints().add(RouteEndpoint.SOURCE);
        }
        if (!destination.isConfirmed()) {
            route.getMissingEndpoints().add(RouteEndpoint.DESTINATION);
        }

        List<RouteStopDTO> stops = new ArrayList<>();
        int included = 0;
        int skipped = 0;
        for (TripStop stop : tripStopRepository.findByTrip_TripIdOrderBySequenceOrderAsc(trip.getTripId())) {
            RouteStopDTO dto = new RouteStopDTO();
            dto.setTripStopId(stop.getTripStopId());
            dto.setSequenceOrder(stop.getSequenceOrder());
            dto.setCity(stop.getStopCity());
            dto.setLatitude(stop.getLatitude());
            dto.setLongitude(stop.getLongitude());
            dto.setConfirmed(hasPoint(stop.getLatitude(), stop.getLongitude()));
            boolean include = dto.isConfirmed() && included < properties.getMaxIntermediateStops();
            dto.setIncludedInRoute(include);
            if (include) {
                included++;
            } else {
                skipped++;
            }
            stops.add(dto);
        }
        route.setStops(stops);
        route.setStopsSkipped(skipped);
        return route;
    }

    private static void applyRoute(TripRouteDTO route, RouteCache.Entry entry) {
        ComputedRoute computed = entry.route();
        route.setRouteStatus(RouteStatus.READY);
        route.setDistanceMeters(computed.distanceMeters());
        route.setDurationSeconds(computed.durationSeconds());
        route.setEncodedPolyline(computed.encodedPolyline());
        if (computed.legs() != null && computed.legs().size() > 1) {
            route.setLegs(computed.legs().stream()
                    .map(l -> new RouteLegDTO(l.distanceMeters(), l.durationSeconds()))
                    .toList());
        }
        route.setCalculatedAt(entry.calculatedAt());
        if (route.getStopsSkipped() != null && route.getStopsSkipped() > 0) {
            route.setMessage(route.getStopsSkipped() + " stop(s) without a confirmed location are not included in this route.");
        }
    }

    static String cacheKey(LatLng origin, LatLng destination, List<LatLng> intermediates) {
        return origin.cacheKey() + "|" + intermediates.stream().map(LatLng::cacheKey).collect(Collectors.joining(";"))
                + "|" + destination.cacheKey();
    }

    static GeocodeCandidateDTO toCandidate(GoogleGeocodingClient.Candidate c) {
        GeocodeCandidateDTO dto = new GeocodeCandidateDTO();
        dto.setFormattedAddress(c.formattedAddress());
        dto.setLatitude(c.latitude());
        dto.setLongitude(c.longitude());
        dto.setLocationType(c.locationType());
        dto.setTypes(c.types());
        dto.setPartialMatch(c.partialMatch());
        dto.setPlaceId(c.placeId());
        boolean cityLevel = c.types() != null && c.types().stream().anyMatch(CITY_LEVEL_TYPES::contains);
        dto.setApproximate(cityLevel || "APPROXIMATE".equals(c.locationType()));
        return dto;
    }

    private static boolean hasPoint(Double lat, Double lng) {
        return lat != null && lng != null;
    }

    private static String missingMessage(List<RouteEndpoint> missing) {
        String which = missing.stream()
                .map(e -> e == RouteEndpoint.SOURCE ? "source" : "destination")
                .collect(Collectors.joining(" and "));
        return "Confirm the " + which + " location to see the planned route.";
    }

    private static RouteStatus toRouteStatus(MapsProviderException e) {
        return switch (e.getReason()) {
            case NOT_CONFIGURED, ACCESS_DENIED -> RouteStatus.NOT_CONFIGURED;
            case RATE_LIMITED -> RouteStatus.RATE_LIMITED;
            case INVALID_REQUEST -> RouteStatus.NO_ROUTE;
            case TIMEOUT, UNAVAILABLE -> RouteStatus.PROVIDER_UNAVAILABLE;
        };
    }

    private static String userMessage(MapsProviderException e) {
        return switch (e.getReason()) {
            case NOT_CONFIGURED, ACCESS_DENIED -> "The map route service is not available right now. Please contact support.";
            case RATE_LIMITED -> "The map route service is busy. Please try again in a few minutes.";
            case INVALID_REQUEST -> "A route could not be calculated for these locations.";
            case TIMEOUT, UNAVAILABLE -> "The map route service is temporarily unavailable. Please try again.";
        };
    }

    private static GenericDataDTO notFound(GenericDataDTO response, Long tripId) {
        response.setResponseCode(CommonStatusCode.NOT_FOUND);
        response.setResponseMessege("Trip not found with ID: " + tripId);
        return response;
    }

    private static GenericDataDTO respond(GenericDataDTO response, Integer code, TripRouteDTO route) {
        response.setResponseCode(code);
        response.setResponseMessege(route.getMessage() != null ? route.getMessage() : "Trip route fetched successfully!");
        response.setData(route);
        return response;
    }
}
