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
@RequestMapping(BaseURL.BASE_URL + MarketplaceUrlConstants.MARKETPLACE + MarketplaceUrlConstants.LOADS)
public class MarketplaceLoadController {

    private final ListingService listingService;

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_LOAD_CREATE + "')")
    public ResponseEntity<?> createLoad(@AuthenticationPrincipal LoggedInUser user, @RequestBody CreateLoadPostRequest request) {
        log.info("API: Create Load Post by staff: {}", user.getStaffId());
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = listingService.createLoadPost(user, request);
        } catch (Exception e) {
            log.error("Error creating load post: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_LOAD_READ + "')")
    public ResponseEntity<?> searchLoads(LoadPostSearchRequest searchRequest) {
        log.info("API: Search Loads");
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = listingService.searchLoads(searchRequest);
        } catch (Exception e) {
            log.error("Error searching loads: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_LOAD_READ + "')")
    public ResponseEntity<?> getLoad(@PathVariable Long id) {
        log.info("API: Get Load Post ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = listingService.getLoadPost(id);
        } catch (Exception e) {
            log.error("Error getting load post {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_LOAD_UPDATE + "')")
    public ResponseEntity<?> updateLoad(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id, @RequestBody CreateLoadPostRequest request) {
        log.info("API: Update Load Post ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = listingService.updateLoadPost(user, id, request);
        } catch (Exception e) {
            log.error("Error updating load post {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_LOAD_CREATE + "')")
    public ResponseEntity<?> publishLoad(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        log.info("API: Publish Load Post ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = listingService.publishListing(user, id);
        } catch (Exception e) {
            log.error("Error publishing load post {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_LOAD_CREATE + "')")
    public ResponseEntity<?> closeLoad(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        log.info("API: Close Load Post ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = listingService.closeListing(user, id);
        } catch (Exception e) {
            log.error("Error closing load post {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
