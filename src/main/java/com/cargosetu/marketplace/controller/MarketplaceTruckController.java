package com.cargosetu.marketplace.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants.MarketplaceUrlConstants;
import com.cargosetu.marketplace.dto.*;
import com.cargosetu.marketplace.service.ListingService;
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
@RequestMapping(BaseURL.BASE_URL + MarketplaceUrlConstants.MARKETPLACE + MarketplaceUrlConstants.TRUCKS)
public class MarketplaceTruckController {

    private final ListingService listingService;

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_TRUCK_CREATE + "')")
    public ResponseEntity<?> createTruck(@AuthenticationPrincipal LoggedInUser user, @RequestBody CreateTruckPostRequest request) {
        log.info("API: Create Truck Post by staff: {}", user.getStaffId());
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = listingService.createTruckPost(user, request);
        } catch (Exception e) {
            log.error("Error creating truck post: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_TRUCK_READ + "')")
    public ResponseEntity<?> searchTrucks(TruckPostSearchRequest searchRequest) {
        log.info("API: Search Trucks");
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = listingService.searchTrucks(searchRequest);
        } catch (Exception e) {
            log.error("Error searching trucks: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
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
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_TRUCK_UPDATE + "')")
    public ResponseEntity<?> updateTruck(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id, @RequestBody CreateTruckPostRequest request) {
        log.info("API: Update Truck Post ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = listingService.updateTruckPost(user, id, request);
        } catch (Exception e) {
            log.error("Error updating truck post {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
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
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
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
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
