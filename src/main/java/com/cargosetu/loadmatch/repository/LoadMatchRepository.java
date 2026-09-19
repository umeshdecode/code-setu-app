package com.cargosetu.loadmatch.repository;

import com.cargosetu.loadmatch.entity.LoadMatch;
import com.cargosetu.loadmatch.entity.LoadMatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoadMatchRepository extends JpaRepository<LoadMatch, Long> {

    List<LoadMatch> findByShipment_ShipmentIdAndStatusIn(Long shipmentId, List<LoadMatchStatus> statuses);

    List<LoadMatch> findByTrip_TripIdAndStatusIn(Long tripId, List<LoadMatchStatus> statuses);

    Optional<LoadMatch> findByShipment_ShipmentIdAndTrip_TripId(Long shipmentId, Long tripId);

    // Unused this phase - cheap to add now for the Phase 3 expiry sweep.
    List<LoadMatch> findByStatusInAndExpiresAtBefore(List<LoadMatchStatus> statuses, LocalDateTime cutoff);

    /**
     * Backs GET /load-matching. Filters existing suggestions directly rather
     * than triggering fresh scoring (fresh scoring only happens via
     * findMatchingTripsForShipment / findMatchingShipmentsForTrip).
     */
    @Query("SELECT lm FROM LoadMatch lm JOIN lm.shipment s JOIN lm.trip t " +
            "WHERE (:sourceCity IS NULL OR LOWER(s.sourceCity) = LOWER(:sourceCity)) " +
            "AND (:destinationCity IS NULL OR LOWER(s.destinationCity) = LOWER(:destinationCity)) " +
            "AND (:minAvailableVolume IS NULL OR t.remainingVolumeCbm >= :minAvailableVolume) " +
            "AND (:minAvailableWeight IS NULL OR t.remainingWeightKg >= :minAvailableWeight) " +
            "ORDER BY lm.overallScore DESC")
    List<LoadMatch> search(@Param("sourceCity") String sourceCity,
                            @Param("destinationCity") String destinationCity,
                            @Param("minAvailableVolume") Double minAvailableVolume,
                            @Param("minAvailableWeight") Double minAvailableWeight);
}
