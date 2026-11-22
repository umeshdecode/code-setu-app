package com.cargosetu.marketplace.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants.MarketplaceUrlConstants;
import com.cargosetu.marketplace.dto.*;
import com.cargosetu.marketplace.service.MarketplaceTripService;
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
@RequestMapping(BaseURL.BASE_URL + MarketplaceUrlConstants.MARKETPLACE + MarketplaceUrlConstants.TRIPS)
public class MarketplaceTripController {

    private final MarketplaceTripService marketplaceTripService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_TRIP_READ + "')")
    public ResponseEntity<?> getTrip(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        log.info("API entry for getTrip id: {}", id);
        try {
            GenericDataDTO response = marketplaceTripService.getTrip(id);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in getTrip", e);
            return MarketplaceResponses.error();
        }
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_TRIP_READ + "')")
    public ResponseEntity<?> getTripTimeline(@PathVariable Long id) {
        log.info("API entry for getTripTimeline id: {}", id);
        try {
            GenericDataDTO response = marketplaceTripService.getTripTimeline(id);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in getTripTimeline", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping("/{id}/events")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_TRIP_UPDATE + "')")
    public ResponseEntity<?> addTripEvent(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id, @Valid @RequestBody TripEventRequest request) {
        log.info("API entry for addTripEvent id: {}", id);
        try {
            GenericDataDTO response = marketplaceTripService.addTripEvent(user, id, request);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in addTripEvent", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping("/{id}/location")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_TRIP_UPDATE + "')")
    public ResponseEntity<?> updateLocation(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id, @Valid @RequestBody LocationUpdateRequest request) {
        log.info("API entry for updateLocation id: {}", id);
        try {
            GenericDataDTO response = marketplaceTripService.updateLocation(user, id, request);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in updateLocation", e);
            return MarketplaceResponses.error();
        }
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_TRIP_READ + "')")
    public ResponseEntity<?> getActiveTrips(@AuthenticationPrincipal LoggedInUser user) {
        log.info("API entry for getActiveTrips user: {}", user.getStaffId());
        try {
            GenericDataDTO response = marketplaceTripService.getActiveTrips(user);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in getActiveTrips", e);
            return MarketplaceResponses.error();
        }
    }
}
