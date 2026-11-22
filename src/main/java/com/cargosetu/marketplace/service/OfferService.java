package com.cargosetu.marketplace.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.marketplace.dto.CounterOfferRequest;
import com.cargosetu.marketplace.dto.CreateOfferRequest;
import com.cargosetu.marketplace.dto.OfferResponse;
import com.cargosetu.marketplace.entity.MarketplaceBooking;
import com.cargosetu.marketplace.entity.MarketplaceListing;
import com.cargosetu.marketplace.entity.MarketplaceOffer;
import com.cargosetu.marketplace.enums.ListingStatus;
import com.cargosetu.marketplace.enums.OfferStatus;
import com.cargosetu.marketplace.mapper.OfferMapper;
import com.cargosetu.marketplace.repository.MarketplaceBookingRepository;
import com.cargosetu.marketplace.repository.MarketplaceListingRepository;
import com.cargosetu.marketplace.repository.MarketplaceOfferRepository;
import com.cargosetu.marketplace.util.MarketplaceResponses;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Offer negotiation.
 *
 * <p>State machine: every negotiation step is its own row. A bid starts {@code PENDING}. When the
 * party who is awaited counters, the current row becomes {@code COUNTERED} and a new {@code PENDING}
 * child row (parentOfferId = previous) is created by the counter-party. Only the {@code PENDING} row
 * at the tip of the chain is actionable, and only by the company that did <em>not</em> author it.
 * Accepting creates a confirmed booking, books the listing and rejects every competing offer.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OfferService {

    /** Default validity of a bid / counter when the client does not send one. */
    private static final long DEFAULT_OFFER_VALIDITY_HOURS = 48;
    private static final Set<ListingStatus> OPEN_LISTING_STATUSES = EnumSet.of(ListingStatus.PUBLISHED, ListingStatus.NEGOTIATING);

    private final MarketplaceOfferRepository offerRepository;
    private final MarketplaceListingRepository listingRepository;
    private final MarketplaceBookingRepository bookingRepository;
    private final OfferMapper offerMapper;
    private final MarketplaceAuditService auditService;
    private final BookingService bookingService;
    private final com.cargosetu.repo.CompanyRepository companyRepository;
    private final com.cargosetu.repo.StaffRepository staffRepository;

    // ───────────────────────────── create ─────────────────────────────

    @Transactional
    public GenericDataDTO createOffer(LoggedInUser user, Long listingId, CreateOfferRequest request) {
        try {
            if (user == null || user.getCompanyId() == null) {
                return MarketplaceResponses.fail(CommonStatusCode.UNAUTHORIZED, "Authentication required");
            }
            if (request == null || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return MarketplaceResponses.fail(CommonStatusCode.FAIL, "Offer amount must be greater than zero");
            }
            if (request.getAdvancePercentage() != null && (request.getAdvancePercentage() < 0 || request.getAdvancePercentage() > 100)) {
                return MarketplaceResponses.fail(CommonStatusCode.FAIL, "Advance percentage must be between 0 and 100");
            }

            MarketplaceListing listing = listingRepository.findByIdWithLock(listingId).orElse(null);
            if (listing == null) {
                return MarketplaceResponses.fail(CommonStatusCode.NOT_FOUND, "Listing not found");
            }
            if (!OPEN_LISTING_STATUSES.contains(listing.getStatus()) || isExpired(listing)) {
                return MarketplaceResponses.fail(CommonStatusCode.CONFLICT, "Listing is no longer open for offers");
            }
            if (Objects.equals(listing.getCompanyId(), user.getCompanyId())) {
                return MarketplaceResponses.fail(CommonStatusCode.FORBIDDEN, "You cannot bid on your own listing");
            }
            boolean hasOpenNegotiation = offerRepository.findByListingIdAndStatus(listingId, OfferStatus.PENDING).stream()
                    .anyMatch(o -> Objects.equals(resolveBidderCompanyId(o), user.getCompanyId()));
            if (hasOpenNegotiation) {
                return MarketplaceResponses.fail(CommonStatusCode.CONFLICT,
                        "You already have an open offer on this listing. Respond to it or wait for the owner.");
            }

            MarketplaceOffer offer = offerMapper.toEntity(request);
            offer.setListing(listing);
            offer.setBidderCompanyId(user.getCompanyId());
            offer.setOfferedByStaffId(user.getStaffId());
            offer.setStatus(OfferStatus.PENDING);
            offer.setValidUntil(resolveValidity(request.getValidUntil(), listing));
            offer = offerRepository.save(offer);

            listing.setStatus(ListingStatus.NEGOTIATING);
            listingRepository.save(listing);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceOffer", offer.getId(), "CREATE", null, "PENDING");

            GenericDataDTO response = new GenericDataDTO();
            response.setData(enrichOfferResponse(offer));
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Offer submitted successfully");
            return response;
        } catch (Exception e) {
            MarketplaceResponses.rollback();
            log.error("Error creating offer", e);
            return MarketplaceResponses.fail(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to create offer");
        }
    }

    // ───────────────────────────── accept ─────────────────────────────

    @Transactional
    public GenericDataDTO acceptOffer(LoggedInUser user, Long offerId) {
        try {
            if (user == null || user.getCompanyId() == null) {
                return MarketplaceResponses.fail(CommonStatusCode.UNAUTHORIZED, "Authentication required");
            }
            MarketplaceOffer offer = offerRepository.findById(offerId).orElse(null);
            if (offer == null || offer.getListing() == null) {
                return MarketplaceResponses.fail(CommonStatusCode.NOT_FOUND, "Offer not found");
            }
            // Lock the listing so two concurrent accepts cannot both succeed.
            MarketplaceListing listing = listingRepository.findByIdWithLock(offer.getListing().getId()).orElse(offer.getListing());

            GenericDataDTO guard = checkActionable(user, offer, listing, "accept");
            if (guard != null) return guard;

            Integer bidderCompanyId = resolveBidderCompanyId(offer);
            offer.setStatus(OfferStatus.ACCEPTED);
            offerRepository.save(offer);

            MarketplaceBooking booking = bookingService.createBookingFromAcceptedOffer(offer, listing, bidderCompanyId, user);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceOffer", offer.getId(), "ACCEPT", "PENDING", "ACCEPTED");

            OfferResponse dto = enrichOfferResponse(offer);
            dto.setBookingId(booking.getId());
            GenericDataDTO response = new GenericDataDTO();
            response.setData(dto);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Offer accepted. Booking " + booking.getBookingNumber() + " is confirmed."
                    + (booking.getVehicleId() == null || booking.getDriverId() == null
                        ? " The transporter now needs to assign a vehicle and driver to start the trip." : ""));
            return response;
        } catch (Exception e) {
            MarketplaceResponses.rollback();
            log.error("Error accepting offer", e);
            return MarketplaceResponses.fail(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to accept offer");
        }
    }

    // ───────────────────────────── reject / withdraw ─────────────────────────────

    @Transactional
    public GenericDataDTO rejectOffer(LoggedInUser user, Long offerId) {
        try {
            if (user == null || user.getCompanyId() == null) {
                return MarketplaceResponses.fail(CommonStatusCode.UNAUTHORIZED, "Authentication required");
            }
            MarketplaceOffer offer = offerRepository.findById(offerId).orElse(null);
            if (offer == null || offer.getListing() == null) {
                return MarketplaceResponses.fail(CommonStatusCode.NOT_FOUND, "Offer not found");
            }
            MarketplaceListing listing = offer.getListing();
            if (offer.getStatus() != OfferStatus.PENDING) {
                return MarketplaceResponses.fail(CommonStatusCode.CONFLICT,
                        "This offer is already " + offer.getStatus().name().toLowerCase());
            }
            Integer author = resolveAuthorCompanyId(offer);
            Integer awaiting = resolveAwaitingCompanyId(offer);
            boolean isAuthor = Objects.equals(author, user.getCompanyId());
            boolean isAwaited = Objects.equals(awaiting, user.getCompanyId());
            if (!isAuthor && !isAwaited) {
                return MarketplaceResponses.fail(CommonStatusCode.FORBIDDEN, "Only negotiation participants can reject this offer");
            }

            // The author pulling back their own offer is a withdrawal, not a rejection.
            OfferStatus newStatus = isAuthor ? OfferStatus.WITHDRAWN : OfferStatus.REJECTED;
            offer.setStatus(newStatus);
            offerRepository.save(offer);
            reopenIfNoPendingOffers(listing);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceOffer", offer.getId(),
                    newStatus.name(), "PENDING", newStatus.name());

            GenericDataDTO response = new GenericDataDTO();
            response.setData(enrichOfferResponse(offer));
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege(isAuthor ? "Offer withdrawn" : "Offer rejected");
            return response;
        } catch (Exception e) {
            MarketplaceResponses.rollback();
            log.error("Error rejecting offer", e);
            return MarketplaceResponses.fail(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to reject offer");
        }
    }

    // ───────────────────────────── counter ─────────────────────────────

    @Transactional
    public GenericDataDTO counterOffer(LoggedInUser user, Long offerId, CounterOfferRequest request) {
        try {
            if (user == null || user.getCompanyId() == null) {
                return MarketplaceResponses.fail(CommonStatusCode.UNAUTHORIZED, "Authentication required");
            }
            if (request == null || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return MarketplaceResponses.fail(CommonStatusCode.FAIL, "Counter offer amount must be greater than zero");
            }
            MarketplaceOffer original = offerRepository.findById(offerId).orElse(null);
            if (original == null || original.getListing() == null) {
                return MarketplaceResponses.fail(CommonStatusCode.NOT_FOUND, "Offer not found");
            }
            MarketplaceListing listing = listingRepository.findByIdWithLock(original.getListing().getId()).orElse(original.getListing());

            GenericDataDTO guard = checkActionable(user, original, listing, "counter");
            if (guard != null) return guard;

            original.setStatus(OfferStatus.COUNTERED);
            offerRepository.save(original);

            MarketplaceOffer counter = new MarketplaceOffer();
            counter.setListing(listing);
            counter.setParentOfferId(original.getId());
            counter.setStatus(OfferStatus.PENDING);
            counter.setAmount(request.getAmount());
            counter.setPaymentTerms(request.getPaymentTerms() != null ? request.getPaymentTerms() : original.getPaymentTerms());
            counter.setAdvancePercentage(request.getAdvancePercentage() != null ? request.getAdvancePercentage() : original.getAdvancePercentage());
            counter.setMessage(request.getMessage());
            Integer bidder = resolveBidderCompanyId(original);
            counter.setBidderCompanyId(bidder != null ? bidder : user.getCompanyId());
            counter.setOfferedByStaffId(user.getStaffId());
            counter.setValidUntil(resolveValidity(null, listing));
            counter = offerRepository.save(counter);

            auditService.logEvent(user.getCompanyId(), user.getStaffId(), "MarketplaceOffer", counter.getId(), "COUNTER", "PENDING", "COUNTERED");

            GenericDataDTO response = new GenericDataDTO();
            response.setData(enrichOfferResponse(counter));
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Counter offer sent");
            return response;
        } catch (Exception e) {
            MarketplaceResponses.rollback();
            log.error("Error in counter offer", e);
            return MarketplaceResponses.fail(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to counter offer");
        }
    }

    // ───────────────────────────── reads ─────────────────────────────

    /**
     * Offers on a listing. The listing owner sees every bid; anyone else only sees their own
     * negotiation, so competitors can no longer read each other's prices.
     */
    public GenericDataDTO getOffersForListing(LoggedInUser user, Long listingId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null) {
                return MarketplaceResponses.fail(CommonStatusCode.UNAUTHORIZED, "Authentication required");
            }
            MarketplaceListing listing = listingRepository.findById(listingId).orElse(null);
            if (listing == null) {
                return MarketplaceResponses.fail(CommonStatusCode.NOT_FOUND, "Listing not found");
            }
            boolean isOwner = Objects.equals(listing.getCompanyId(), user.getCompanyId());
            List<OfferResponse> list = offerRepository.findByListingId(listingId).stream()
                    .filter(o -> isOwner || Objects.equals(resolveBidderCompanyId(o), user.getCompanyId()))
                    .sorted(NEWEST_FIRST)
                    .map(this::enrichOfferResponse)
                    .toList();
            response.setDataList(list);
            response.setTotalRecords(list.size());
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Offers retrieved successfully");
        } catch (Exception e) {
            log.error("Error fetching offers for listing {}", listingId, e);
            return MarketplaceResponses.fail(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to get offers");
        }
        return response;
    }

    /** All offers received on the caller's own listings, in one call (replaces one call per listing). */
    public GenericDataDTO getIncomingOffers(LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null) {
                return MarketplaceResponses.fail(CommonStatusCode.UNAUTHORIZED, "Authentication required");
            }
            List<OfferResponse> list = offerRepository.findByListingCompanyIdOrderByCreatedAtDesc(user.getCompanyId()).stream()
                    .map(this::enrichOfferResponse)
                    .toList();
            response.setDataList(list);
            response.setTotalRecords(list.size());
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Incoming offers retrieved successfully");
        } catch (Exception e) {
            log.error("Error fetching incoming offers for company {}", user != null ? user.getCompanyId() : null, e);
            return MarketplaceResponses.fail(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to get incoming offers");
        }
        return response;
    }

    public GenericDataDTO getMyOffers(LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null) {
                return MarketplaceResponses.fail(CommonStatusCode.UNAUTHORIZED, "Authentication required");
            }
            List<MarketplaceOffer> offers = offerRepository.findByBidderCompanyId(user.getCompanyId());
            List<OfferResponse> list = (offers != null ? offers : Collections.<MarketplaceOffer>emptyList())
                    .stream()
                    .sorted(NEWEST_FIRST)
                    .map(this::enrichOfferResponse)
                    .toList();
            response.setDataList(list);
            response.setTotalRecords(list.size());
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("My offers retrieved successfully");
        } catch (Exception e) {
            log.error("Error fetching offers for company {}", user != null ? user.getCompanyId() : null, e);
            return MarketplaceResponses.fail(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to get my offers");
        }
        return response;
    }

    /** The whole negotiation (root bid and every counter) that {@code offerId} belongs to, oldest first. */
    public GenericDataDTO getOfferThread(LoggedInUser user, Long offerId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user == null || user.getCompanyId() == null) {
                return MarketplaceResponses.fail(CommonStatusCode.UNAUTHORIZED, "Authentication required");
            }
            MarketplaceOffer offer = offerRepository.findById(offerId).orElse(null);
            if (offer == null || offer.getListing() == null) {
                return MarketplaceResponses.fail(CommonStatusCode.NOT_FOUND, "Offer not found");
            }
            Integer owner = offer.getListing().getCompanyId();
            Integer bidder = resolveBidderCompanyId(offer);
            if (!Objects.equals(user.getCompanyId(), owner) && !Objects.equals(user.getCompanyId(), bidder)) {
                return MarketplaceResponses.fail(CommonStatusCode.FORBIDDEN, "Only negotiation participants can view this thread");
            }
            List<MarketplaceOffer> all = offerRepository.findByListingId(offer.getListing().getId());
            Map<Long, MarketplaceOffer> byId = new HashMap<>();
            all.forEach(o -> byId.put(o.getId(), o));
            Long rootId = offer.getId();
            int guard = 0;
            while (byId.containsKey(rootId) && byId.get(rootId).getParentOfferId() != null && guard++ < 50) {
                rootId = byId.get(rootId).getParentOfferId();
            }
            List<MarketplaceOffer> thread = new ArrayList<>();
            MarketplaceOffer current = byId.get(rootId);
            guard = 0;
            while (current != null && guard++ < 50) {
                thread.add(current);
                final Long parent = current.getId();
                current = all.stream().filter(o -> parent.equals(o.getParentOfferId())).findFirst().orElse(null);
            }
            response.setDataList(thread.stream().map(this::enrichOfferResponse).toList());
            response.setTotalRecords(thread.size());
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Negotiation history retrieved");
        } catch (Exception e) {
            log.error("Error fetching offer thread {}", offerId, e);
            return MarketplaceResponses.fail(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to get negotiation history");
        }
        return response;
    }

    // ───────────────────────────── expiry ─────────────────────────────

    /** Every 15 minutes, expire PENDING offers past their validity and reopen listings with no live offers. */
    @Scheduled(fixedDelayString = "${marketplace.offer-expiry.interval-ms:900000}", initialDelay = 60000)
    @Transactional
    public void expireStaleOffers() {
        try {
            List<MarketplaceOffer> stale = offerRepository.findByStatusAndValidUntilBefore(OfferStatus.PENDING, LocalDateTime.now());
            if (stale.isEmpty()) return;
            // Keyed by id: entity hashCode (Lombok @Data) walks the listing<->post relation.
            Map<Long, MarketplaceListing> touched = new HashMap<>();
            for (MarketplaceOffer o : stale) {
                o.setStatus(OfferStatus.EXPIRED);
                offerRepository.save(o);
                if (o.getListing() != null) touched.put(o.getListing().getId(), o.getListing());
            }
            touched.values().forEach(this::reopenIfNoPendingOffers);
            log.info("Expired {} stale marketplace offers", stale.size());
        } catch (Exception e) {
            log.error("Offer expiry job failed", e);
        }
    }

    // ───────────────────────────── helpers ─────────────────────────────

    private static final Comparator<MarketplaceOffer> NEWEST_FIRST = (a, b) -> {
        if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
            int cmp = b.getCreatedAt().compareTo(a.getCreatedAt());
            if (cmp != 0) return cmp;
        }
        if (a.getId() != null && b.getId() != null) return b.getId().compareTo(a.getId());
        return 0;
    };

    /**
     * Shared guard for accept / counter: the offer must be the live PENDING tip, not expired, the
     * listing still open, and the caller must be the party being waited on (never the author).
     */
    private GenericDataDTO checkActionable(LoggedInUser user, MarketplaceOffer offer, MarketplaceListing listing, String action) {
        if (offer.getStatus() != OfferStatus.PENDING) {
            return MarketplaceResponses.fail(CommonStatusCode.CONFLICT,
                    "Only the latest pending offer can be " + (action.equals("accept") ? "accepted" : "countered")
                            + " (this one is " + offer.getStatus().name().toLowerCase() + ")");
        }
        if (offer.getValidUntil() != null && offer.getValidUntil().isBefore(LocalDateTime.now())) {
            offer.setStatus(OfferStatus.EXPIRED);
            offerRepository.save(offer);
            return MarketplaceResponses.fail(CommonStatusCode.CONFLICT, "This offer has expired");
        }
        if (listing.getStatus() != null && !OPEN_LISTING_STATUSES.contains(listing.getStatus())) {
            return MarketplaceResponses.fail(CommonStatusCode.CONFLICT, "Listing is no longer open for negotiation");
        }
        if (isExpired(listing)) {
            return MarketplaceResponses.fail(CommonStatusCode.CONFLICT, "Listing has expired");
        }
        Integer author = resolveAuthorCompanyId(offer);
        Integer awaiting = resolveAwaitingCompanyId(offer);
        if (Objects.equals(author, user.getCompanyId())) {
            return MarketplaceResponses.fail(CommonStatusCode.FORBIDDEN,
                    "You can't " + action + " your own offer; waiting for the other party");
        }
        if (!Objects.equals(awaiting, user.getCompanyId())) {
            return MarketplaceResponses.fail(CommonStatusCode.FORBIDDEN, "Only negotiation participants can " + action + " this offer");
        }
        return null;
    }

    private boolean isExpired(MarketplaceListing listing) {
        return listing.getValidUntil() != null && listing.getValidUntil().isBefore(LocalDateTime.now());
    }

    private LocalDateTime resolveValidity(LocalDateTime requested, MarketplaceListing listing) {
        LocalDateTime validity = requested != null ? requested : LocalDateTime.now().plusHours(DEFAULT_OFFER_VALIDITY_HOURS);
        if (listing.getValidUntil() != null && validity.isAfter(listing.getValidUntil())) {
            validity = listing.getValidUntil();
        }
        return validity;
    }

    private void reopenIfNoPendingOffers(MarketplaceListing listing) {
        if (listing == null || listing.getStatus() != ListingStatus.NEGOTIATING) return;
        if (offerRepository.countByListingIdAndStatus(listing.getId(), OfferStatus.PENDING) == 0) {
            listing.setStatus(ListingStatus.PUBLISHED);
            listingRepository.save(listing);
        }
    }

    /** Company that authored this row: the staff member's company, else inferred from the chain. */
    private Integer resolveAuthorCompanyId(MarketplaceOffer offer) {
        if (offer == null) return null;
        if (offer.getOfferedByStaffId() != null) {
            Integer staffCompany = staffRepository.findById(offer.getOfferedByStaffId())
                    .map(com.cargosetu.model.Staff::getCompanyId).orElse(null);
            if (staffCompany != null) return staffCompany;
        }
        Integer owner = offer.getListing() != null ? offer.getListing().getCompanyId() : null;
        Integer bidder = resolveBidderCompanyId(offer);
        if (offer.getParentOfferId() == null) return bidder;
        // Counters alternate between bidder and owner.
        MarketplaceOffer parent = offerRepository.findById(offer.getParentOfferId()).orElse(null);
        Integer parentAuthor = parent != null ? resolveAuthorCompanyId(parent) : null;
        return Objects.equals(parentAuthor, owner) ? bidder : owner;
    }

    /** Company that is expected to respond to a PENDING offer, or null if the offer is closed. */
    private Integer resolveAwaitingCompanyId(MarketplaceOffer offer) {
        if (offer == null || offer.getStatus() != OfferStatus.PENDING || offer.getListing() == null) return null;
        Integer owner = offer.getListing().getCompanyId();
        Integer bidder = resolveBidderCompanyId(offer);
        return Objects.equals(resolveAuthorCompanyId(offer), owner) ? bidder : owner;
    }

    private Integer resolveBidderCompanyId(MarketplaceOffer offer) {
        MarketplaceOffer current = offer;
        int depth = 0;
        while (current != null && depth < 10) {
            Integer listingOwner = current.getListing() != null ? current.getListing().getCompanyId() : null;
            if (current.getBidderCompanyId() != null && !current.getBidderCompanyId().equals(listingOwner)) {
                return current.getBidderCompanyId();
            }
            if (current.getParentOfferId() == null) break;
            current = offerRepository.findById(current.getParentOfferId()).orElse(null);
            depth++;
        }
        return offer != null ? offer.getBidderCompanyId() : null;
    }

    private OfferResponse enrichOfferResponse(MarketplaceOffer offer) {
        if (offer == null) return null;
        OfferResponse mapped = offerMapper.toDto(offer);
        final OfferResponse dto = mapped != null ? mapped : new OfferResponse();
        dto.setId(offer.getId());
        dto.setBidderCompanyId(resolveBidderCompanyId(offer));
        dto.setOfferedByStaffId(offer.getOfferedByStaffId());
        dto.setAmount(offer.getAmount());
        dto.setPaymentTerms(offer.getPaymentTerms());
        dto.setAdvancePercentage(offer.getAdvancePercentage());
        dto.setValidUntil(offer.getValidUntil());
        dto.setStatus(offer.getStatus());
        dto.setMessage(offer.getMessage());
        dto.setParentOfferId(offer.getParentOfferId());
        dto.setCreatedAt(offer.getCreatedAt());
        dto.setUpdatedAt(offer.getUpdatedAt());
        dto.setCreatedByCompanyId(resolveAuthorCompanyId(offer));
        dto.setAwaitingCompanyId(resolveAwaitingCompanyId(offer));
        if (offer.getStatus() == OfferStatus.ACCEPTED && offer.getId() != null) {
            bookingRepository.findFirstByOfferId(offer.getId()).ifPresent(b -> dto.setBookingId(b.getId()));
        }

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
            if (offer.getStatus() == OfferStatus.COUNTERED && offer.getId() != null && listing.getId() != null) {
                offerRepository.findByListingId(listing.getId()).stream()
                        .filter(o -> offer.getId().equals(o.getParentOfferId()))
                        .findFirst()
                        .ifPresent(child -> {
                            dto.setLatestCounterOfferId(child.getId());
                            dto.setLatestCounterAmount(child.getAmount());
                            dto.setLatestCounterStatus(child.getStatus());
                        });
            }
        }
        Integer bidder = dto.getBidderCompanyId();
        if (bidder != null) {
            companyRepository.findById(bidder).ifPresent(c -> dto.setBidderCompanyName(c.getCompanyName()));
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
