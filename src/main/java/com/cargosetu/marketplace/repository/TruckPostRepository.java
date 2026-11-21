package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.TruckPost;
import com.cargosetu.marketplace.enums.TruckAvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TruckPostRepository extends JpaRepository<TruckPost, Long>, JpaSpecificationExecutor<TruckPost> {
    Optional<TruckPost> findByListingId(Long listingId);
    List<TruckPost> findByListingStatus(com.cargosetu.marketplace.enums.ListingStatus status);
    List<TruckPost> findByVehicleIdAndTruckStatusNot(Long vehicleId, TruckAvailabilityStatus status);
    List<TruckPost> findByCurrentCityAndTruckStatus(String currentCity, TruckAvailabilityStatus status);
    List<TruckPost> findByRouteToAndAvailableCapacityGreaterThanEqual(String routeTo, Double capacity);
}
