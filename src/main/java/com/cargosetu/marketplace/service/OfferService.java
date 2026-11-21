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
    private final com.cargosetu.repo.CompanyRepository companyRepository;
    private final com.cargosetu.repo.StaffRepository staffRepository;

    @Transactional
    public GenericDataDTO createOffer(LoggedInUser user, Long listingId, CreateOfferRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (request == null || request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Offer amount must be greater than zero");
                return response;
            }

            MarketplaceListing listing = listingRepository.findById(listingId).orElse(null);
            if (listing == null || (listing.getStatus() != com.cargosetu.marketplace.enums.ListingStatus.PUBLISHED
                    && listing.getStatus() != com.cargosetu.marketplace.enums.ListingStatus.NEGOTIATING)) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Listing not found or not active for offers");
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

            response.setData(enrichOfferResponse(offer));
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Offer created successfully");
        } catch (Exception e) {
            log.error("Error creating offer", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to create offer: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public GenericDataDTO acceptOffer(LoggedInUser user, Long offerId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null) {
                response.setResponseCode(CommonStatusCode.UNAUTHORIZED);
                response.setResponseMessege("Authentication required");
                return response;
            }

            MarketplaceOffer offer = offerRepository.findById(offerId).orElse(null);
            if (offer == null || offer.getListing() == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Offer not found");
                return response;
            }

            boolean isListingOwner = user.getCompanyId().equals(offer.getListing().getCompanyId());
            Integer resolvedBidderId = resolveBidderCompanyId(offer);
            boolean isBidderRespondingToCounter = offer.getParentOfferId() != null
                    && resolvedBidderId != null
                    && resolvedBidderId.equals(user.getCompanyId())
                    && !isOfferCreatedByCompany(offer, user.getCompanyId());

            if (!isListingOwner && !isBidderRespondingToCounter) {
                response.setResponseCode(CommonStatusCode.FORBIDDEN);
                response.setResponseMessege("Unauthorized: only the listing owner (or counter-offer recipient) can accept offers");
                return response;
            }

            if (offer.getStatus() != com.cargosetu.marketplace.enums.OfferStatus.PENDING) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Only PENDING offers can be accepted (current status: " + offer.getStatus() + ")");
                return response;
            }

            com.cargosetu.marketplace.enums.ListingStatus listingStatus = offer.getListing().getStatus();
            if (listingStatus != null
                    && listingStatus != com.cargosetu.marketplace.enums.ListingStatus.PUBLISHED
                    && listingStatus != com.cargosetu.marketplace.enums.ListingStatus.NEGOTIATING) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Listing is no longer open for accepting offers");
                return response;
            }

            offer.setStatus(com.cargosetu.marketplace.enums.OfferStatus.ACCEPTED);
            offerRepository.save(offer);

            // Reject other pending offers on this listing
            List<MarketplaceOffer> otherOffers = offerRepository.findByListingIdAndStatus(
                    offer.getListing().getId(), com.cargosetu.marketplace.enums.OfferStatus.PENDING);
            if (otherOffers != null) {
                for (MarketplaceOffer other : otherOffers) {
                    if (!other.getId().equals(offer.getId())) {
                        other.setStatus(com.cargosetu.marketplace.enums.OfferStatus.REJECTED);
                        offerRepository.save(other);
                    }
                }
            }

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceOffer", offer.getId(), "ACCEPT", "PENDING", "ACCEPTED");

            response.setData(enrichOfferResponse(offer));
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Offer accepted successfully");
        } catch (Exception e) {
            log.error("Error accepting offer", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to accept offer: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public GenericDataDTO rejectOffer(LoggedInUser user, Long offerId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null) {
                response.setResponseCode(CommonStatusCode.UNAUTHORIZED);
                response.setResponseMessege("Authentication required");
                return response;
            }

            MarketplaceOffer offer = offerRepository.findById(offerId).orElse(null);
            if (offer == null || offer.getListing() == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Offer not found");
                return response;
            }

            boolean isListingOwner = user.getCompanyId().equals(offer.getListing().getCompanyId());
            Integer resolvedBidderId = resolveBidderCompanyId(offer);
            boolean isBidderRespondingToCounter = offer.getParentOfferId() != null
                    && resolvedBidderId != null
                    && resolvedBidderId.equals(user.getCompanyId())
                    && !isOfferCreatedByCompany(offer, user.getCompanyId());

            if (!isListingOwner && !isBidderRespondingToCounter) {
                response.setResponseCode(CommonStatusCode.FORBIDDEN);
                response.setResponseMessege("Unauthorized: only the listing owner (or counter-offer recipient) can reject offers");
                return response;
            }

            if (offer.getStatus() != com.cargosetu.marketplace.enums.OfferStatus.PENDING) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Only PENDING offers can be rejected (current status: " + offer.getStatus() + ")");
                return response;
            }

            offer.setStatus(com.cargosetu.marketplace.enums.OfferStatus.REJECTED);
            offerRepository.save(offer);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceOffer", offer.getId(), "REJECT", "PENDING", "REJECTED");

            response.setData(enrichOfferResponse(offer));
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Offer rejected");
        } catch (Exception e) {
            log.error("Error rejecting offer", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to reject offer: " + e.getMessage());
        }
        return response;
    }

    private boolean isOfferCreatedByCompany(MarketplaceOffer offer, Integer companyId) {
        if (offer == null || companyId == null) return false;
        if (offer.getOfferedByStaffId() != null) {
            com.cargosetu.model.Staff staff = staffRepository.findById(offer.getOfferedByStaffId()).orElse(null);
            if (staff != null && staff.getCompanyId() != null) {
                return companyId.equals(staff.getCompanyId());
            }
        }
        if (offer.getParentOfferId() == null) {
            return companyId.equals(offer.getBidderCompanyId());
        }
        return false;
    }

    @Transactional
    public GenericDataDTO counterOffer(LoggedInUser user, Long offerId, CounterOfferRequest request) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null) {
                response.setResponseCode(CommonStatusCode.UNAUTHORIZED);
                response.setResponseMessege("Authentication required");
                return response;
            }

            if (request == null || request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Counter offer amount must be greater than zero");
                return response;
            }

            MarketplaceOffer originalOffer = offerRepository.findById(offerId).orElse(null);
            if (originalOffer == null || originalOffer.getListing() == null) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Offer not found");
                return response;
            }

            MarketplaceListing listing = originalOffer.getListing();
            Integer listingOwnerCompanyId = listing.getCompanyId();
            Integer bidderCompanyId = resolveBidderCompanyId(originalOffer);

            boolean isListingOwner = listingOwnerCompanyId != null && listingOwnerCompanyId.equals(user.getCompanyId());
            boolean isBidder = bidderCompanyId != null && bidderCompanyId.equals(user.getCompanyId());

            if (!isListingOwner && !isBidder) {
                response.setResponseCode(CommonStatusCode.FORBIDDEN);
                response.setResponseMessege("Unauthorized: only negotiation participants can counter this offer");
                return response;
            }

            if (originalOffer.getStatus() != com.cargosetu.marketplace.enums.OfferStatus.PENDING) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Only PENDING offers can be countered (current status: " + originalOffer.getStatus() + ")");
                return response;
            }

            if (listing.getStatus() != null
                    && listing.getStatus() != com.cargosetu.marketplace.enums.ListingStatus.PUBLISHED
                    && listing.getStatus() != com.cargosetu.marketplace.enums.ListingStatus.NEGOTIATING) {
                response.setResponseCode(CommonStatusCode.FAIL);
                response.setResponseMessege("Listing is no longer open for negotiation");
                return response;
            }

            originalOffer.setStatus(com.cargosetu.marketplace.enums.OfferStatus.COUNTERED);
            offerRepository.save(originalOffer);

            MarketplaceOffer counter = new MarketplaceOffer();
            counter.setListing(listing);
            counter.setParentOfferId(originalOffer.getId());
            counter.setStatus(com.cargosetu.marketplace.enums.OfferStatus.PENDING);
            counter.setAmount(request.getAmount());
            counter.setPaymentTerms(request.getPaymentTerms() != null ? request.getPaymentTerms() : originalOffer.getPaymentTerms());
            counter.setMessage(request.getMessage());
            counter.setBidderCompanyId(bidderCompanyId != null ? bidderCompanyId : user.getCompanyId());
            counter.setOfferedByStaffId(user.getStaffId());
            counter = offerRepository.save(counter);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceOffer", counter.getId(), "COUNTER", "PENDING", "COUNTERED");

            response.setData(enrichOfferResponse(counter));
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Counter offer created");
        } catch (Exception e) {
            log.error("Error in counter offer", e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to counter offer: " + e.getMessage());
        }
        return response;
    }

    private Integer resolveBidderCompanyId(MarketplaceOffer offer) {
        MarketplaceOffer current = offer;
        int depth = 0;
        while (current != null && depth < 10) {
            Integer listingOwner = current.getListing() != null ? current.getListing().getCompanyId() : null;
            if (current.getBidderCompanyId() != null && !current.getBidderCompanyId().equals(listingOwner)) {
                return current.getBidderCompanyId();
            }
            if (current.getParentOfferId() == null) {
                break;
            }
            current = offerRepository.findById(current.getParentOfferId()).orElse(null);
            depth++;
        }
        return offer != null ? offer.getBidderCompanyId() : null;
    }

    public GenericDataDTO getOffersForListing(Long listingId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            java.util.List<com.cargosetu.marketplace.dto.OfferResponse> list = offerRepository.findByListingId(listingId).stream()
                    .map(this::enrichOfferResponse)
                    .toList();
            response.setDataList(list);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Offers retrieved successfully");
        } catch (Exception e) {
            log.error("Error fetching offers for listing {}", listingId, e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to get offers: " + e.getMessage());
        }
        return response;
    }

    public GenericDataDTO getMyOffers(LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null) {
                response.setResponseCode(CommonStatusCode.UNAUTHORIZED);
                response.setResponseMessege("Authentication required");
                return response;
            }
            java.util.List<MarketplaceOffer> offers = offerRepository.findByBidderCompanyId(user.getCompanyId());
            java.util.List<com.cargosetu.marketplace.dto.OfferResponse> list = (offers != null ? offers : java.util.Collections.<MarketplaceOffer>emptyList())
                    .stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                            int cmp = b.getCreatedAt().compareTo(a.getCreatedAt());
                            if (cmp != 0) return cmp;
                        }
                        if (a.getId() != null && b.getId() != null) {
                            return b.getId().compareTo(a.getId());
                        }
                        return 0;
                    })
                    .map(this::enrichOfferResponse)
                    .toList();
            response.setDataList(list);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("My offers retrieved successfully");
        } catch (Exception e) {
            log.error("Error fetching offers for company {}", user != null ? user.getCompanyId() : null, e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Failed to get my offers: " + e.getMessage());
        }
        return response;
    }

    private com.cargosetu.marketplace.dto.OfferResponse enrichOfferResponse(MarketplaceOffer offer) {
        if (offer == null) return null;
        com.cargosetu.marketplace.dto.OfferResponse mapped = offerMapper.toDto(offer);
        final com.cargosetu.marketplace.dto.OfferResponse dto = mapped != null ? mapped : new com.cargosetu.marketplace.dto.OfferResponse();
        dto.setId(offer.getId());
        dto.setBidderCompanyId(offer.getBidderCompanyId());
        dto.setOfferedByStaffId(offer.getOfferedByStaffId());
        dto.setAmount(offer.getAmount());
        dto.setPaymentTerms(offer.getPaymentTerms());
        dto.setValidUntil(offer.getValidUntil());
        dto.setStatus(offer.getStatus());
        dto.setMessage(offer.getMessage());
        dto.setParentOfferId(offer.getParentOfferId());
        dto.setCreatedAt(offer.getCreatedAt());
        dto.setUpdatedAt(offer.getUpdatedAt());

        if (offer.getListing() != null) {
            MarketplaceListing listing = offer.getListing();
            dto.setListingId(listing.getId());
            dto.setListingNumber(listing.getListingNumber());
            dto.setListingType(listing.getType());
            dto.setListingStatus(listing.getStatus());
            dto.setListingOwnerCompanyId(listing.getCompanyId());
            dto.setListingPostedByStaffId(listing.getPostedByStaffId());
            if (listing.getCompanyId() != null) {
                companyRepository.findById(listing.getCompanyId())
                        .ifPresent(c -> dto.setListingOwnerCompanyName(c.getCompanyName()));
            }
            if (listing.getLoadPost() != null) {
                com.cargosetu.marketplace.entity.LoadPost lp = listing.getLoadPost();
                dto.setOriginCity(lp.getOriginCity());
                dto.setOriginState(lp.getOriginState());
                dto.setDestinationCity(lp.getDestinationCity());
                dto.setDestinationState(lp.getDestinationState());
                dto.setListingAskingPrice(lp.getAskingPrice());
                dto.setCargoType(lp.getCargoType());
                dto.setWeight(lp.getWeight());
                dto.setWeightUnit(lp.getWeightUnit());
            } else if (listing.getTruckPost() != null) {
                com.cargosetu.marketplace.entity.TruckPost tp = listing.getTruckPost();
                dto.setOriginCity(tp.getRouteFrom() != null && !tp.getRouteFrom().isBlank() ? tp.getRouteFrom() : tp.getCurrentCity());
                dto.setOriginState(tp.getRouteFromState() != null ? tp.getRouteFromState() : tp.getCurrentState());
                dto.setDestinationCity(tp.getRouteTo());
                dto.setDestinationState(tp.getRouteToState());
                dto.setWeight(tp.getAvailableCapacity());
                dto.setWeightUnit(tp.getCapacityUnit());
            }
            if (offer.getStatus() == com.cargosetu.marketplace.enums.OfferStatus.COUNTERED && offer.getId() != null && listing.getId() != null) {
                List<MarketplaceOffer> allForListing = offerRepository.findByListingId(listing.getId());
                if (allForListing != null) {
                    allForListing.stream()
                            .filter(o -> offer.getId().equals(o.getParentOfferId()))
                            .findFirst()
                            .ifPresent(child -> {
                                dto.setLatestCounterOfferId(child.getId());
                                dto.setLatestCounterAmount(child.getAmount());
                                dto.setLatestCounterStatus(child.getStatus());
                            });
                }
            }
        }
        if (offer.getBidderCompanyId() != null) {
            companyRepository.findById(offer.getBidderCompanyId())
                    .ifPresent(c -> dto.setBidderCompanyName(c.getCompanyName()));
        }
        if (offer.getOfferedByStaffId() != null) {
            staffRepository.findById(offer.getOfferedByStaffId())
                    .ifPresent(s -> {
                        String name = s.getFirstName() != null ? (s.getFirstName() + (s.getLastName() != null ? " " + s.getLastName() : "")) : s.getUserName();
                        dto.setOfferedByName(name);
                    });
        }
        return dto;
    }
}
