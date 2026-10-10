package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.dto.CounterOfferRequest;
import com.cargosetu.marketplace.dto.CreateOfferRequest;
import com.cargosetu.marketplace.entity.MarketplaceListing;
import com.cargosetu.marketplace.entity.MarketplaceOffer;
import com.cargosetu.marketplace.mapper.OfferMapper;
import com.cargosetu.marketplace.repository.MarketplaceListingRepository;
import com.cargosetu.marketplace.repository.MarketplaceOfferRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OfferService {

    private final MarketplaceOfferRepository offerRepository;
    private final MarketplaceListingRepository listingRepository;
    private final OfferMapper offerMapper;
    private final MarketplaceAuditService auditService;

    @Transactional
    public GenericDataDTO createOffer(LoggedInUser user, Long listingId, CreateOfferRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceListing listing = listingRepository.findById(listingId).orElse(null);
            if (listing == null || listing.getStatus() != com.cargosetu.marketplace.enums.ListingStatus.PUBLISHED) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Listing not found or not published");
                return response;
            }

            if (listing.getCompanyId().equals(user.getCompanyId())) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Cannot bid on own listing");
                return response;
            }

            MarketplaceOffer offer = offerMapper.toEntity(request);
            offer.setListing(listing);
            offer.setBidderCompanyId(user.getCompanyId());
            offer.setOfferedByStaffId(user.getStaffId());
            offer.setStatus(com.cargosetu.marketplace.enums.OfferStatus.PENDING);
            offer = offerRepository.save(offer);

            listing.setStatus(com.cargosetu.marketplace.enums.ListingStatus.NEGOTIATING);
            listingRepository.save(listing);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceOffer", offer.getId(), "CREATE", null, "PENDING");

            response.setData(offerMapper.toDto(offer));
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Offer created successfully");
        } catch (Exception e) {
            log.error("Error creating offer", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to create offer");
        }
        return response;
    }

    @Transactional
    public GenericDataDTO acceptOffer(LoggedInUser user, Long offerId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceOffer offer = offerRepository.findById(offerId).orElse(null);
            if (offer == null || !offer.getListing().getCompanyId().equals(user.getCompanyId())) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Offer not found or unauthorized");
                return response;
            }

            offer.setStatus(com.cargosetu.marketplace.enums.OfferStatus.ACCEPTED);
            offerRepository.save(offer);

            // Reject other offers
            List<MarketplaceOffer> otherOffers = offerRepository.findByListingIdAndStatus(offer.getListing().getId(), com.cargosetu.marketplace.enums.OfferStatus.PENDING);
            for (MarketplaceOffer other : otherOffers) {
                if (!other.getId().equals(offer.getId())) {
                    other.setStatus(com.cargosetu.marketplace.enums.OfferStatus.REJECTED);
                    offerRepository.save(other);
                }
            }

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceOffer", offer.getId(), "ACCEPT", "PENDING", "ACCEPTED");

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Offer accepted successfully");
        } catch (Exception e) {
            log.error("Error accepting offer", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Transactional
    public GenericDataDTO rejectOffer(LoggedInUser user, Long offerId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceOffer offer = offerRepository.findById(offerId).orElse(null);
            if (offer != null) {
                offer.setStatus(com.cargosetu.marketplace.enums.OfferStatus.REJECTED);
                offerRepository.save(offer);
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Offer rejected");
            }
        } catch (Exception e) {
            log.error("Error rejecting offer", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Transactional
    public GenericDataDTO counterOffer(LoggedInUser user, Long offerId, CounterOfferRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            MarketplaceOffer originalOffer = offerRepository.findById(offerId).orElse(null);
            if (originalOffer != null) {
                originalOffer.setStatus(com.cargosetu.marketplace.enums.OfferStatus.COUNTERED);
                offerRepository.save(originalOffer);

                MarketplaceOffer counter = new MarketplaceOffer();
                counter.setListing(originalOffer.getListing());
                counter.setParentOfferId(originalOffer.getId());
                counter.setStatus(com.cargosetu.marketplace.enums.OfferStatus.PENDING);
                counter.setAmount(request.getAmount());
                counter.setBidderCompanyId(user.getCompanyId());
                counter.setOfferedByStaffId(user.getStaffId());
                offerRepository.save(counter);

                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Counter offer created");
            }
        } catch (Exception e) {
            log.error("Error in counter offer", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public GenericDataDTO getOffersForListing(Long listingId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            response.setDataList(offerRepository.findByListingId(listingId));
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public GenericDataDTO getMyOffers(LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            response.setDataList(offerRepository.findByBidderCompanyId(user.getCompanyId()));
            response.setResponseCode(CommonStatusCode.SUCCESS);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }
}
