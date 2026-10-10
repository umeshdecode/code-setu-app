package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.MarketplaceListing;
import com.cargosetu.marketplace.enums.ListingStatus;
import com.cargosetu.marketplace.enums.ListingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketplaceListingRepository extends JpaRepository<MarketplaceListing, Long>, JpaSpecificationExecutor<MarketplaceListing> {
    List<MarketplaceListing> findByCompanyIdAndStatus(Integer companyId, ListingStatus status);
    List<MarketplaceListing> findByCompanyIdAndTypeAndStatus(Integer companyId, ListingType type, ListingStatus status);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT l FROM MarketplaceListing l WHERE l.id = :id")
    Optional<MarketplaceListing> findByIdWithLock(@org.springframework.data.repository.query.Param("id") Long id);
    List<MarketplaceListing> findByTypeAndStatusAndValidUntilAfter(ListingType type, ListingStatus status, LocalDateTime now);
    Optional<MarketplaceListing> findByListingNumber(String listingNumber);
    long countByCompanyIdAndStatus(Integer companyId, ListingStatus status);
}
