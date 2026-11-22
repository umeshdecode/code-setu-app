package com.cargosetu.marketplace.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants.MarketplaceUrlConstants;
import com.cargosetu.marketplace.dto.*;
import com.cargosetu.marketplace.service.ListingService;
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
@RequestMapping(BaseURL.BASE_URL + MarketplaceUrlConstants.MARKETPLACE + MarketplaceUrlConstants.TRUCKS)
public class MarketplaceTruckController {

    private final ListingService listingService;

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_TRUCK_CREATE + "')")
    public ResponseEntity<?> createTruck(@AuthenticationPrincipal LoggedInUser user, @Valid @RequestBody CreateTruckPostRequest request) {
        log.info("API: Create Truck Post by staff: {}", user.getStaffId());
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = listingService.createTruckPost(user, request);
        } catch (Exception e) {
            log.error("Error creating truck post: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(null);
            return MarketplaceResponses.error();
        }
        return MarketplaceResponses.of(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_TRUCK_READ + "')")
    public ResponseEntity<?> searchTrucks(@AuthenticationPrincipal LoggedInUser user, TruckPostSearchRequest searchRequest) {
        log.info("API: Search Trucks");
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = listingService.searchTrucks(searchRequest, user);
        } catch (Exception e) {
            log.error("Error searching trucks: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(null);
            return MarketplaceResponses.error();
        }
        return MarketplaceResponses.of(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_TRUCK_READ + "')")
    public ResponseEntity<?> getMyTrucks(
            @AuthenticationPrincipal LoggedInUser user,
            @RequestParam(required = false) String status) {
        log.info("API: Get My Truck Posts by company: {}", user != null ? user.getCompanyId() : null);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = listingService.getMyTruckPosts(user, status);
        } catch (Exception e) {
            log.error("Error fetching my trucks: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(null);
            return MarketplaceResponses.error();
        }
        return MarketplaceResponses.of(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_TRUCK_READ + "')")
    public ResponseEntity<?> getTruck(@PathVariable Long id) {
        log.info("API: Get Truck Post ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = listingService.getTruckPost(id);
        } catch (Exception e) {
            log.error("Error getting truck post {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(null);
            return MarketplaceResponses.error();
        }
        return MarketplaceResponses.of(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_TRUCK_UPDATE + "')")
    public ResponseEntity<?> updateTruck(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id, @Valid @RequestBody CreateTruckPostRequest request) {
        log.info("API: Update Truck Post ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = listingService.updateTruckPost(user, id, request);
        } catch (Exception e) {
            log.error("Error updating truck post {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(null);
            return MarketplaceResponses.error();
        }
        return MarketplaceResponses.of(response);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_TRUCK_CREATE + "')")
    public ResponseEntity<?> publishTruck(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        log.info("API: Publish Truck Post ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = listingService.publishListing(user, id);
        } catch (Exception e) {
            log.error("Error publishing truck post {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(null);
            return MarketplaceResponses.error();
        }
        return MarketplaceResponses.of(response);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_TRUCK_CREATE + "')")
    public ResponseEntity<?> closeTruck(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        log.info("API: Close Truck Post ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = listingService.closeListing(user, id);
        } catch (Exception e) {
            log.error("Error closing truck post {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(null);
            return MarketplaceResponses.error();
        }
        return MarketplaceResponses.of(response);
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_TRUCK_CREATE + "')")
    public ResponseEntity<?> renew(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        log.info("API: Renew listing ID: {}", id);
        try {
            return MarketplaceResponses.of(listingService.renewListing(user, id));
        } catch (Exception e) {
            log.error("Error renewing listing {}", id, e);
            return MarketplaceResponses.error();
        }
    }
}
