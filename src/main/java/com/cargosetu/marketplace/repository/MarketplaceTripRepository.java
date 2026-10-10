package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.MarketplaceTrip;
import com.cargosetu.marketplace.enums.MarketplaceTripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketplaceTripRepository extends JpaRepository<MarketplaceTrip, Long> {
    Optional<MarketplaceTrip> findByBookingId(Long bookingId);
    Optional<MarketplaceTrip> findByTripNumber(String tripNumber);
    List<MarketplaceTrip> findByVehicleIdAndStatus(Long vehicleId, MarketplaceTripStatus status);
    List<MarketplaceTrip> findByDriverIdAndStatus(Long driverId, MarketplaceTripStatus status);
    List<MarketplaceTrip> findByCompanyIdAndStatusIn(Integer companyId, List<MarketplaceTripStatus> statuses);
}
