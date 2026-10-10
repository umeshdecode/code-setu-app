package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.MktTripLocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MktTripLocationHistoryRepository extends JpaRepository<MktTripLocationHistory, Long> {
    List<MktTripLocationHistory> findByTripIdOrderByCapturedAtDesc(Long tripId);
    Optional<MktTripLocationHistory> findFirstByTripIdOrderByCapturedAtDesc(Long tripId);
    List<MktTripLocationHistory> findByVehicleIdAndCapturedAtBetween(Long vehicleId, LocalDateTime from, LocalDateTime to);
}
