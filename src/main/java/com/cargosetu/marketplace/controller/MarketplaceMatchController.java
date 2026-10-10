package com.cargosetu.marketplace.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants.MarketplaceUrlConstants;
import com.cargosetu.marketplace.dto.*;
import com.cargosetu.marketplace.service.MatchingService;
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
@RequestMapping(BaseURL.BASE_URL + MarketplaceUrlConstants.MARKETPLACE + MarketplaceUrlConstants.MATCHES)
public class MarketplaceMatchController {

    private final MatchingService matchingService;

    @GetMapping("/load/{loadPostId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_MATCH_READ + "')")
    public ResponseEntity<?> findMatchesForLoad(@PathVariable Long loadPostId) {
        log.info("API entry for findMatchesForLoad loadPostId: {}", loadPostId);
        try {
            GenericDataDTO response = matchingService.findMatchesForLoad(loadPostId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in findMatchesForLoad", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to find matches", null, new Object[]{e.getMessage()}));
        }
    }

    @GetMapping("/truck/{truckPostId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_MATCH_READ + "')")
    public ResponseEntity<?> findMatchesForTruck(@PathVariable Long truckPostId) {
        log.info("API entry for findMatchesForTruck truckPostId: {}", truckPostId);
        try {
            GenericDataDTO response = matchingService.findMatchesForTruck(truckPostId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in findMatchesForTruck", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to find matches", null, new Object[]{e.getMessage()}));
        }
    }

    @GetMapping("/recommendations")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_MATCH_READ + "')")
    public ResponseEntity<?> getRecommendations(@AuthenticationPrincipal LoggedInUser user) {
        log.info("API entry for getRecommendations user: {}", user.getStaffId());
        try {
            GenericDataDTO response = matchingService.getRecommendations(user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in getRecommendations", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to get recommendations", null, new Object[]{e.getMessage()}));
        }
    }

    @PostMapping(MarketplaceUrlConstants.ALERTS)
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_MATCH_CREATE + "')")
    public ResponseEntity<?> createSavedSearch(@AuthenticationPrincipal LoggedInUser user, @RequestBody SavedSearchRequest request) {
        log.info("API entry for createSavedSearch user: {}", user.getStaffId());
        try {
            GenericDataDTO response = matchingService.createSavedSearch(user, request);
            return ResponseEntity.status(CommonStatusCode.CREATED).body(response);
        } catch (Exception e) {
            log.error("Exception in createSavedSearch", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to create saved search", null, new Object[]{e.getMessage()}));
        }
    }

    @GetMapping(MarketplaceUrlConstants.ALERTS)
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_MATCH_READ + "')")
    public ResponseEntity<?> getMySavedSearches(@AuthenticationPrincipal LoggedInUser user) {
        log.info("API entry for getMySavedSearches user: {}", user.getStaffId());
        try {
            GenericDataDTO response = matchingService.getMySavedSearches(user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in getMySavedSearches", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to get saved searches", null, new Object[]{e.getMessage()}));
        }
    }
}
