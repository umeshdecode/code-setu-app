package com.cargosetu.loadmatch.service.matching;

import com.cargosetu.loadmatch.entity.Shipment;
import com.cargosetu.loadmatch.entity.TripStop;
import com.cargosetu.loadmatch.repository.TripStopRepository;
import com.cargosetu.model.Trip;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure-ish scoring function for "how good is this Shipment <-> Trip pairing".
 * A Spring @Component (rather than a static/plain class) because it needs
 * TripStopRepository to evaluate the route sub-score against a trip's
 * intermediate stops - there is no other stateless-helper-as-bean precedent
 * in this module to follow, but wiring it through Spring keeps it consistent
 * with the rest of the service layer and still leaves `score()` a simple,
 * easily testable method with no other side effects.
 */
@Component
public class MatchScoringEngine {

    // Timing tolerance: a shipment pickup date within this many days of the
    // trip's planned window still earns partial credit (Phase 0 decision:
    // simple date-window heuristic, not a routing/ETA engine).
    private static final int TIMING_TOLERANCE_DAYS = 2;

    private final TripStopRepository tripStopRepository;

    public MatchScoringEngine(TripStopRepository tripStopRepository) {
        this.tripStopRepository = tripStopRepository;
    }

    public MatchScoreResult score(Shipment shipment, Trip trip) {
        boolean eligible = isEligible(shipment, trip);
        if (!eligible) {
            return new MatchScoreResult(0, 0, 0, 0, 0, 0, 0, false);
        }

        double routeScore = scoreRoute(shipment, trip);
        double capacityScore = scoreCapacity(shipment, trip);
        double weightScore = scoreWeight(shipment, trip);
        double timingScore = scoreTiming(shipment, trip);
        double vehicleScore = scoreVehicle(shipment, trip);
        double handlingScore = scoreHandling(shipment, trip);

        double overallScore = (routeScore * MatchWeights.ROUTE
                + capacityScore * MatchWeights.CAPACITY
                + weightScore * MatchWeights.WEIGHT
                + timingScore * MatchWeights.TIMING
                + vehicleScore * MatchWeights.VEHICLE
                + handlingScore * MatchWeights.HANDLING) * 100.0;

        return new MatchScoreResult(routeScore, capacityScore, weightScore, timingScore,
                vehicleScore, handlingScore, overallScore, true);
    }

    /**
     * Hard gate: a shipment that doesn't fit AT ALL shouldn't be suggested.
     * Fails closed - any missing capacity data is treated as NOT eligible
     * rather than assuming infinite capacity.
     */
    private boolean isEligible(Shipment shipment, Trip trip) {
        if (shipment.getTotalWeightKg() == null || shipment.getTotalVolumeCbm() == null) {
            return false;
        }
        if (trip.getRemainingWeightKg() == null || trip.getRemainingVolumeCbm() == null) {
            return false;
        }
        return shipment.getTotalWeightKg() <= trip.getRemainingWeightKg()
                && shipment.getTotalVolumeCbm() <= trip.getRemainingVolumeCbm();
    }

    /**
     * Placeholder route heuristic (Phase 0 decision #2) - a future upgrade can
     * replace this with real geo-distance/routing. For now: exact, trimmed,
     * case-insensitive city-name comparison against the trip's effective
     * origin/destination (from its Shifting, if any) and its declared stops.
     */
    private double scoreRoute(Shipment shipment, Trip trip) {
        String shipmentSource = normalize(shipment.getSourceCity());
        String shipmentDestination = normalize(shipment.getDestinationCity());

        String tripOrigin = null;
        String tripDestination = null;
        if (trip.getShifting() != null) {
            tripOrigin = normalize(trip.getShifting().getShiftFromCity());
            tripDestination = normalize(trip.getShifting().getShiftToCity());
        }

        List<TripStop> stops = trip.getTripId() != null
                ? tripStopRepository.findByTrip_TripIdOrderBySequenceOrderAsc(trip.getTripId())
                : List.of();
        Set<String> stopCities = stops.stream()
                .map(stop -> normalize(stop.getStopCity()))
                .filter(city -> city != null && !city.isEmpty())
                .collect(Collectors.toSet());

        boolean originMatches = shipmentSource != null && shipmentSource.equals(tripOrigin);
        boolean destinationMatches = shipmentDestination != null
                && (shipmentDestination.equals(tripDestination) || stopCities.contains(shipmentDestination));

        if (originMatches && destinationMatches) {
            return 1.0;
        }
        if (destinationMatches) {
            return 0.5;
        }
        return 0.0;
    }

    /**
     * Ratio-based: the eligibility gate already rejected anything that
     * doesn't fit, so here we simply reward shipments that use up more of the
     * remaining volume (closer to fully utilizing the trip), clamped to
     * [0, 1]. Simpler than an "ideal utilization target" curve, and good
     * enough for a Phase 2 placeholder.
     */
    private double scoreCapacity(Shipment shipment, Trip trip) {
        return clamp01(shipment.getTotalVolumeCbm() / trip.getRemainingVolumeCbm());
    }

    private double scoreWeight(Shipment shipment, Trip trip) {
        return clamp01(shipment.getTotalWeightKg() / trip.getRemainingWeightKg());
    }

    private double scoreTiming(Shipment shipment, Trip trip) {
        LocalDate pickupDate = shipment.getRequiredPickupDate();
        LocalDateTime plannedStart = trip.getPlannedStartTime();
        LocalDateTime plannedEnd = trip.getPlannedEenTime(); // pre-existing typo'd field name, kept as-is

        if (pickupDate == null || plannedStart == null || plannedEnd == null) {
            return 0.0;
        }

        LocalDate windowStart = plannedStart.toLocalDate();
        LocalDate windowEnd = plannedEnd.toLocalDate();
        if (!pickupDate.isBefore(windowStart) && !pickupDate.isAfter(windowEnd)) {
            return 1.0;
        }

        long daysFromWindow = pickupDate.isBefore(windowStart)
                ? java.time.temporal.ChronoUnit.DAYS.between(pickupDate, windowStart)
                : java.time.temporal.ChronoUnit.DAYS.between(windowEnd, pickupDate);
        if (daysFromWindow <= TIMING_TOLERANCE_DAYS) {
            return 0.5;
        }
        return 0.0;
    }

    private double scoreVehicle(Shipment shipment, Trip trip) {
        String required = shipment.getVehicleTypeRequired();
        if (required == null || required.trim().isEmpty()) {
            return 1.0;
        }
        return normalize(required).equals(normalize(trip.getVehicleTypeCategory())) ? 1.0 : 0.0;
    }

    private double scoreHandling(Shipment shipment, Trip trip) {
        Set<String> requiredTags = parseTags(shipment.getSpecialHandling());
        if (requiredTags.isEmpty()) {
            return 1.0;
        }
        Set<String> availableTags = parseTags(trip.getEquipmentTags());
        if (availableTags.isEmpty()) {
            return 0.0;
        }
        long matched = requiredTags.stream().filter(availableTags::contains).count();
        if (matched == requiredTags.size()) {
            return 1.0;
        }
        if (matched == 0) {
            return 0.0;
        }
        return (double) matched / requiredTags.size();
    }

    private Set<String> parseTags(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.trim().isEmpty()) {
            return new HashSet<>();
        }
        return Arrays.stream(commaSeparated.split(","))
                .map(this::normalize)
                .filter(tag -> tag != null && !tag.isEmpty())
                .collect(Collectors.toSet());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private double clamp01(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    @Getter
    public static class MatchScoreResult {
        private final double routeScore;
        private final double capacityScore;
        private final double weightScore;
        private final double timingScore;
        private final double vehicleScore;
        private final double handlingScore;
        private final double overallScore;
        private final boolean eligible;

        public MatchScoreResult(double routeScore, double capacityScore, double weightScore,
                                 double timingScore, double vehicleScore, double handlingScore,
                                 double overallScore, boolean eligible) {
            this.routeScore = routeScore;
            this.capacityScore = capacityScore;
            this.weightScore = weightScore;
            this.timingScore = timingScore;
            this.vehicleScore = vehicleScore;
            this.handlingScore = handlingScore;
            this.overallScore = overallScore;
            this.eligible = eligible;
        }
    }
}
