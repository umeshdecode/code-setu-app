package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.LoadPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoadPostRepository extends JpaRepository<LoadPost, Long>, JpaSpecificationExecutor<LoadPost> {
    Optional<LoadPost> findByListingId(Long listingId);
    List<LoadPost> findByListingStatus(com.cargosetu.marketplace.enums.ListingStatus status);
    List<LoadPost> findByOriginCityAndDestinationCity(String originCity, String destinationCity);
    
    @Query("SELECT l FROM LoadPost l WHERE l.originCity = :originCity AND l.destinationCity = :destinationCity AND l.pickupFrom >= :startDate AND l.pickupFrom <= :endDate AND l.cargoType = :cargoType")
    List<LoadPost> findAvailableLoads(@Param("originCity") String originCity, @Param("destinationCity") String destinationCity, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("cargoType") com.cargosetu.marketplace.enums.CargoType cargoType);
}
