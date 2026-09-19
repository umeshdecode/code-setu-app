package com.cargosetu.loadmatch.service;

import com.cargosetu.utils.GenericDataDTO;
import org.springframework.stereotype.Service;

@Service
public interface LoadMatchingService {

    /**
     * Scores every plausible Trip candidate against the given Shipment,
     * upserts a LoadMatch row (status=SUGGESTED) for each eligible result,
     * and returns them sorted by overallScore descending.
     */
    GenericDataDTO findMatchingTripsForShipment(Long shipmentId);

    /**
     * Symmetric to findMatchingTripsForShipment: scores every plausible
     * Shipment candidate against the given Trip.
     */
    GenericDataDTO findMatchingShipmentsForTrip(Long tripId);

    /**
     * Filters existing LoadMatch suggestions by route/capacity criteria.
     * Does NOT trigger fresh scoring - that only happens via the two
     * find*For* methods above.
     */
    GenericDataDTO search(String sourceCity, String destinationCity, Double minAvailableVolume, Double minAvailableWeight);

    /**
     * Idempotent SUGGESTED -> VIEWED transition. Leaves VIEWED/terminal rows
     * untouched rather than erroring.
     */
    GenericDataDTO markViewed(Long loadMatchId);

    /**
     * SUGGESTED/VIEWED -> REQUESTED transition: staff has formally asked to
     * put this shipment on this trip. Any other current status is a
     * validation error (returned as GenericDataDTO, not thrown).
     */
    GenericDataDTO requestMatch(Long loadMatchId, Long staffId);

    /**
     * SUGGESTED/VIEWED/REQUESTED -> REJECTED transition. Stores the optional
     * reason on the LoadMatch row. Any other current status is a validation
     * error.
     */
    GenericDataDTO rejectMatch(Long loadMatchId, Long staffId, String reason);

    /**
     * The critical transactional path: REQUESTED -> ACCEPTED, decrementing
     * the Trip's remaining capacity and creating the TripShipment manifest
     * row. Safe under concurrent staff usage via Trip's @Version optimistic
     * lock - see implementation for the full mechanics.
     */
    GenericDataDTO acceptMatch(Long loadMatchId, Long staffId);

    /**
     * Scheduled sweep: moves any SUGGESTED/VIEWED/REQUESTED match whose
     * expiresAt has passed to EXPIRED. Invoked hourly by
     * LoadMatchExpirySweepJob.
     */
    void sweepExpiredMatches();
}
