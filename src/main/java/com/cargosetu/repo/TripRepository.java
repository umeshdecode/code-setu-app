package com.cargosetu.repo;

import com.cargosetu.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    // Smart Load Matching: cheap DB-level pre-filter for candidate trips that
    // could plausibly carry a given shipment, used by LoadMatchingService
    // before running the full scoring engine over each candidate.
    List<Trip> findByRemainingWeightKgGreaterThanEqualAndRemainingVolumeCbmGreaterThanEqual(
            Double remainingWeightKg, Double remainingVolumeCbm);
}
