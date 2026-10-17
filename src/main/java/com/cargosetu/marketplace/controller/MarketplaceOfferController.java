package com.cargosetu.marketplace.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants.MarketplaceUrlConstants;
import com.cargosetu.marketplace.dto.*;
import com.cargosetu.marketplace.service.OfferService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.utils.PermissionConstants;
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
    public ResponseEntity<?> createOffer(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long listingId, @RequestBody CreateOfferRequest request) {
        log.info("API entry for createOffer for listingId: {}", listingId);
        try {
            GenericDataDTO response = offerService.createOffer(user, listingId, request);
            return ResponseEntity.status(CommonStatusCode.CREATED).body(response);
        } catch (Exception e) {
            log.error("Exception in createOffer", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to create offer", null, new Object[]{e.getMessage()}));
        }
    }

    @PostMapping(MarketplaceUrlConstants.OFFERS)
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_OFFER_CREATE + "')")
    public ResponseEntity<?> createOfferDirect(@AuthenticationPrincipal LoggedInUser user, @RequestParam(required = false) Long listingId, @RequestBody CreateOfferRequest request) {
        log.info("API entry for createOfferDirect listingId: {}", listingId);
        if (listingId == null) {
            return ResponseEntity.badRequest().body(new GenericDataDTO(CommonStatusCode.FAIL, "listingId parameter is required to submit an offer", null, null));
        }
        try {
            GenericDataDTO response = offerService.createOffer(user, listingId, request);
            return ResponseEntity.status(CommonStatusCode.CREATED).body(response);
        } catch (Exception e) {
            log.error("Exception in createOfferDirect", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to create offer", null, new Object[]{e.getMessage()}));
        }
    }

    @GetMapping(MarketplaceUrlConstants.LISTINGS + "/{listingId}" + MarketplaceUrlConstants.OFFERS)
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_OFFER_READ + "')")
    public ResponseEntity<?> getOffersForListing(@PathVariable Long listingId) {
        log.info("API entry for getOffersForListing listingId: {}", listingId);
        try {
            GenericDataDTO response = offerService.getOffersForListing(listingId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in getOffersForListing", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to get offers", null, new Object[]{e.getMessage()}));
        }
    }

    @GetMapping(MarketplaceUrlConstants.OFFERS + "/listing/{listingId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_OFFER_READ + "')")
    public ResponseEntity<?> getOffersForListingLegacy(@PathVariable Long listingId) {
        log.info("API entry for getOffersForListingLegacy listingId: {}", listingId);
        try {
            GenericDataDTO response = offerService.getOffersForListing(listingId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in getOffersForListingLegacy", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to get offers", null, new Object[]{e.getMessage()}));
        }
    }

    @PostMapping(MarketplaceUrlConstants.OFFERS + "/{offerId}/accept")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_OFFER_CREATE + "')")
    public ResponseEntity<?> acceptOffer(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long offerId) {
        log.info("API entry for acceptOffer offerId: {}", offerId);
        try {
            GenericDataDTO response = offerService.acceptOffer(user, offerId);
            int status = response.getResponseCode() != null ? response.getResponseCode() : CommonStatusCode.SUCCESS;
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            log.error("Exception in acceptOffer", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to accept offer", null, new Object[]{e.getMessage()}));
        }
    }

    @PostMapping(MarketplaceUrlConstants.OFFERS + "/{offerId}/reject")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_OFFER_CREATE + "')")
    public ResponseEntity<?> rejectOffer(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long offerId) {
        log.info("API entry for rejectOffer offerId: {}", offerId);
        try {
            GenericDataDTO response = offerService.rejectOffer(user, offerId);
            int status = response.getResponseCode() != null ? response.getResponseCode() : CommonStatusCode.SUCCESS;
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            log.error("Exception in rejectOffer", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to reject offer", null, new Object[]{e.getMessage()}));
        }
    }

    @PostMapping(MarketplaceUrlConstants.OFFERS + "/{offerId}/counter")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_OFFER_CREATE + "')")
    public ResponseEntity<?> counterOffer(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long offerId, @RequestBody CounterOfferRequest request) {
        log.info("API entry for counterOffer offerId: {}", offerId);
        try {
            GenericDataDTO response = offerService.counterOffer(user, offerId, request);
            int status = response.getResponseCode() != null ? response.getResponseCode() : CommonStatusCode.SUCCESS;
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            log.error("Exception in counterOffer", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to counter offer", null, new Object[]{e.getMessage()}));
        }
    }
}
