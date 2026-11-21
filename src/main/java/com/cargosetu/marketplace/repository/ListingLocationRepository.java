package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.ListingLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingLocationRepository extends JpaRepository<ListingLocation, Long> {
    List<ListingLocation> findByListingIdOrderBySequence(Long listingId);
    void deleteByListingId(Long listingId);
}
