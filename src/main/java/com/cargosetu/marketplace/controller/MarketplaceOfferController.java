package com.cargosetu.marketplace.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants.MarketplaceUrlConstants;
import com.cargosetu.marketplace.dto.*;
import com.cargosetu.marketplace.service.OfferService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.marketplace.util.MarketplaceResponses;
import com.cargosetu.utils.PermissionConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(BaseURL.BASE_URL + MarketplaceUrlConstants.MARKETPLACE)
public class MarketplaceOfferController {

    private final OfferService offerService;

    @PostMapping(MarketplaceUrlConstants.LISTINGS + "/{listingId}" + MarketplaceUrlConstants.OFFERS)
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_OFFER_CREATE + "')")
    public ResponseEntity<?> createOffer(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long listingId, @Valid @RequestBody CreateOfferRequest request) {
        log.info("API entry for createOffer for listingId: {}", listingId);
        try {
            GenericDataDTO response = offerService.createOffer(user, listingId, request);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in createOffer", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping(MarketplaceUrlConstants.OFFERS)
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_OFFER_CREATE + "')")
    public ResponseEntity<?> createOfferDirect(@AuthenticationPrincipal LoggedInUser user, @RequestParam(required = false) Long listingId, @Valid @RequestBody CreateOfferRequest request) {
        log.info("API entry for createOfferDirect listingId: {}", listingId);
        if (listingId == null) {
            return ResponseEntity.badRequest().body(new GenericDataDTO(CommonStatusCode.FAIL, "listingId parameter is required to submit an offer", null, null));
        }
        try {
            GenericDataDTO response = offerService.createOffer(user, listingId, request);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in createOfferDirect", e);
            return MarketplaceResponses.error();
        }
    }

    @GetMapping(MarketplaceUrlConstants.LISTINGS + "/{listingId}" + MarketplaceUrlConstants.OFFERS)
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_OFFER_READ + "')")
    public ResponseEntity<?> getOffersForListing(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long listingId) {
        log.info("API entry for getOffersForListing listingId: {}", listingId);
        try {
            GenericDataDTO response = offerService.getOffersForListing(user, listingId);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in getOffersForListing", e);
            return MarketplaceResponses.error();
        }
    }

    @GetMapping({MarketplaceUrlConstants.OFFERS + "/my", MarketplaceUrlConstants.OFFERS})
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_OFFER_READ + "')")
    public ResponseEntity<?> getMyOffers(@AuthenticationPrincipal LoggedInUser user) {
        log.info("API entry for getMyOffers user: {}", user != null ? user.getStaffId() : null);
        try {
            GenericDataDTO response = offerService.getMyOffers(user);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in getMyOffers", e);
            return MarketplaceResponses.error();
        }
    }

    @GetMapping(MarketplaceUrlConstants.OFFERS + "/listing/{listingId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_OFFER_READ + "')")
    public ResponseEntity<?> getOffersForListingLegacy(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long listingId) {
        log.info("API entry for getOffersForListingLegacy listingId: {}", listingId);
        try {
            GenericDataDTO response = offerService.getOffersForListing(user, listingId);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in getOffersForListingLegacy", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping(MarketplaceUrlConstants.OFFERS + "/{offerId}/accept")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_OFFER_CREATE + "')")
    public ResponseEntity<?> acceptOffer(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long offerId) {
        log.info("API entry for acceptOffer offerId: {}", offerId);
        try {
            GenericDataDTO response = offerService.acceptOffer(user, offerId);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in acceptOffer", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping(MarketplaceUrlConstants.OFFERS + "/{offerId}/reject")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_OFFER_CREATE + "')")
    public ResponseEntity<?> rejectOffer(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long offerId) {
        log.info("API entry for rejectOffer offerId: {}", offerId);
        try {
            GenericDataDTO response = offerService.rejectOffer(user, offerId);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in rejectOffer", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping(MarketplaceUrlConstants.OFFERS + "/{offerId}/counter")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_OFFER_CREATE + "')")
    public ResponseEntity<?> counterOffer(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long offerId, @Valid @RequestBody CounterOfferRequest request) {
        log.info("API entry for counterOffer offerId: {}", offerId);
        try {
            GenericDataDTO response = offerService.counterOffer(user, offerId, request);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in counterOffer", e);
            return MarketplaceResponses.error();
        }
    }

    @GetMapping(MarketplaceUrlConstants.OFFERS + "/incoming")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_OFFER_READ + "')")
    public ResponseEntity<?> getIncomingOffers(@AuthenticationPrincipal LoggedInUser user) {
        log.info("API entry for getIncomingOffers");
        try {
            return MarketplaceResponses.of(offerService.getIncomingOffers(user));
        } catch (Exception e) {
            log.error("Exception in getIncomingOffers", e);
            return MarketplaceResponses.error();
        }
    }

    @GetMapping(MarketplaceUrlConstants.OFFERS + "/{offerId}/thread")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_OFFER_READ + "')")
    public ResponseEntity<?> getOfferThread(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long offerId) {
        log.info("API entry for getOfferThread offerId: {}", offerId);
        try {
            return MarketplaceResponses.of(offerService.getOfferThread(user, offerId));
        } catch (Exception e) {
            log.error("Exception in getOfferThread", e);
            return MarketplaceResponses.error();
        }
    }
}
