package com.cargosetu.marketplace.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants.MarketplaceUrlConstants;
import com.cargosetu.marketplace.dto.*;
import com.cargosetu.marketplace.service.MatchingService;
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
@RequestMapping(BaseURL.BASE_URL + MarketplaceUrlConstants.MARKETPLACE + MarketplaceUrlConstants.MATCHES)
public class MarketplaceMatchController {

    private final MatchingService matchingService;

    @GetMapping("/load/{loadPostId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_MATCH_READ + "')")
    public ResponseEntity<?> findMatchesForLoad(@PathVariable Long loadPostId) {
        log.info("API entry for findMatchesForLoad loadPostId: {}", loadPostId);
        try {
            GenericDataDTO response = matchingService.findMatchesForLoad(loadPostId);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in findMatchesForLoad", e);
            return MarketplaceResponses.error();
        }
    }

    @GetMapping("/truck/{truckPostId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_MATCH_READ + "')")
    public ResponseEntity<?> findMatchesForTruck(@PathVariable Long truckPostId) {
        log.info("API entry for findMatchesForTruck truckPostId: {}", truckPostId);
        try {
            GenericDataDTO response = matchingService.findMatchesForTruck(truckPostId);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in findMatchesForTruck", e);
            return MarketplaceResponses.error();
        }
    }

    @GetMapping("/recommendations")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_MATCH_READ + "')")
    public ResponseEntity<?> getRecommendations(@AuthenticationPrincipal LoggedInUser user) {
        log.info("API entry for getRecommendations user: {}", user.getStaffId());
        try {
            GenericDataDTO response = matchingService.getRecommendations(user);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in getRecommendations", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping(MarketplaceUrlConstants.ALERTS)
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_MATCH_CREATE + "')")
    public ResponseEntity<?> createSavedSearch(@AuthenticationPrincipal LoggedInUser user, @Valid @RequestBody SavedSearchRequest request) {
        log.info("API entry for createSavedSearch user: {}", user.getStaffId());
        try {
            GenericDataDTO response = matchingService.createSavedSearch(user, request);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in createSavedSearch", e);
            return MarketplaceResponses.error();
        }
    }

    @GetMapping(MarketplaceUrlConstants.ALERTS)
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_MATCH_READ + "')")
    public ResponseEntity<?> getMySavedSearches(@AuthenticationPrincipal LoggedInUser user) {
        log.info("API entry for getMySavedSearches user: {}", user.getStaffId());
        try {
            GenericDataDTO response = matchingService.getMySavedSearches(user);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in getMySavedSearches", e);
            return MarketplaceResponses.error();
        }
    }

    @DeleteMapping(MarketplaceUrlConstants.ALERTS + "/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_MATCH_CREATE + "')")
    public ResponseEntity<?> deleteSavedSearch(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        log.info("API entry for deleteSavedSearch id: {}", id);
        try {
            return MarketplaceResponses.of(matchingService.deleteSavedSearch(user, id));
        } catch (Exception e) {
            log.error("Exception in deleteSavedSearch", e);
            return MarketplaceResponses.error();
        }
    }
}
