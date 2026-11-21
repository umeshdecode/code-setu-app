package com.cargosetu.marketplace.repository;

import com.cargosetu.marketplace.entity.MarketplaceOffer;
import com.cargosetu.marketplace.enums.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketplaceOfferRepository extends JpaRepository<MarketplaceOffer, Long> {
    List<MarketplaceOffer> findByListingIdAndStatus(Long listingId, OfferStatus status);
    List<MarketplaceOffer> findByListingId(Long listingId);
    List<MarketplaceOffer> findByBidderCompanyId(Integer companyId);
    Optional<MarketplaceOffer> findByListingIdAndBidderCompanyId(Long listingId, Integer companyId);
    long countByListingIdAndStatus(Long listingId, OfferStatus status);
}
