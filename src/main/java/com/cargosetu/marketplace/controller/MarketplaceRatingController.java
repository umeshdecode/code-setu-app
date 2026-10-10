package com.cargosetu.marketplace.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants.MarketplaceUrlConstants;
import com.cargosetu.marketplace.dto.*;
import com.cargosetu.marketplace.service.RatingService;
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
@RequestMapping(BaseURL.BASE_URL + MarketplaceUrlConstants.MARKETPLACE + MarketplaceUrlConstants.RATINGS)
public class MarketplaceRatingController {

    private final RatingService ratingService;

    @PostMapping("/booking/{bookingId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_RATING_CREATE + "')")
    public ResponseEntity<?> submitRating(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long bookingId, @RequestBody RatingRequest request) {
        log.info("API entry for submitRating bookingId: {}", bookingId);
        try {
            GenericDataDTO response = ratingService.submitRating(user, bookingId, request);
            return ResponseEntity.status(CommonStatusCode.CREATED).body(response);
        } catch (Exception e) {
            log.error("Exception in submitRating", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to submit rating", null, new Object[]{e.getMessage()}));
        }
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_RATING_READ + "')")
    public ResponseEntity<?> getRatingsForBooking(@PathVariable Long bookingId) {
        log.info("API entry for getRatingsForBooking bookingId: {}", bookingId);
        try {
            GenericDataDTO response = ratingService.getRatingsForBooking(bookingId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in getRatingsForBooking", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to get ratings", null, new Object[]{e.getMessage()}));
        }
    }

    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_RATING_READ + "')")
    public ResponseEntity<?> getCompanyRatings(@PathVariable Integer companyId) {
        log.info("API entry for getCompanyRatings companyId: {}", companyId);
        try {
            GenericDataDTO response = ratingService.getCompanyRatings(companyId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in getCompanyRatings", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to get company ratings", null, new Object[]{e.getMessage()}));
        }
    }
}
