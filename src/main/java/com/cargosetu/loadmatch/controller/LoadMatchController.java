package com.cargosetu.loadmatch.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.loadmatch.service.LoadMatchingService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Phase 2 landed the SUGGESTED -> VIEWED -> EXPIRED path. Phase 3 adds the
 * request/accept/reject lifecycle endpoints below, backed by the
 * transactional capacity-locking logic in LoadMatchingServiceImpl.
 */
@RestController
@Slf4j
public class LoadMatchController {

    private final LoadMatchingService loadMatchingService;

    public LoadMatchController(LoadMatchingService loadMatchingService) {
        this.loadMatchingService = loadMatchingService;
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).LOADMATCH_VIEW)")
    @GetMapping(BaseURL.BASE_URL + BaseURL.SHIPMENT + "/{shipmentId}/matching-trips")
    public ResponseEntity<?> getMatchingTripsForShipment(@PathVariable Long shipmentId) {
        log.info("API: Get Matching Trips for Shipment ID: {}", shipmentId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = loadMatchingService.findMatchingTripsForShipment(shipmentId);
        } catch (Exception e) {
            log.error("Error getting matching trips for shipment ID {}: {}", shipmentId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).LOADMATCH_VIEW)")
    @GetMapping(BaseURL.BASE_URL + UrlConstants.TripUrlConstants.TRIP + "/{tripId}/matching-shipments")
    public ResponseEntity<?> getMatchingShipmentsForTrip(@PathVariable Long tripId) {
        log.info("API: Get Matching Shipments for Trip ID: {}", tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = loadMatchingService.findMatchingShipmentsForTrip(tripId);
        } catch (Exception e) {
            log.error("Error getting matching shipments for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).LOADMATCH_VIEW)")
    @GetMapping(BaseURL.BASE_URL + BaseURL.LOAD_MATCHING)
    public ResponseEntity<?> searchLoadMatches(@RequestParam(required = false) String sourceCity,
                                                @RequestParam(required = false) String destinationCity,
                                                @RequestParam(required = false) Double minAvailableVolume,
                                                @RequestParam(required = false) Double minAvailableWeight) {
        log.info("API: Search Load Matches, sourceCity={}, destinationCity={}, minAvailableVolume={}, minAvailableWeight={}",
                sourceCity, destinationCity, minAvailableVolume, minAvailableWeight);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = loadMatchingService.search(sourceCity, destinationCity, minAvailableVolume, minAvailableWeight);
        } catch (Exception e) {
            log.error("Error searching load matches: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).LOADMATCH_VIEW)")
    @PostMapping(BaseURL.BASE_URL + BaseURL.LOAD_MATCHING + "/{loadMatchId}/view")
    public ResponseEntity<?> markViewed(@PathVariable Long loadMatchId) {
        log.info("API: Mark Load Match Viewed, ID: {}", loadMatchId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = loadMatchingService.markViewed(loadMatchId);
        } catch (Exception e) {
            log.error("Error marking load match ID {} as viewed: {}", loadMatchId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).LOADMATCH_REQUEST)")
    @PostMapping(BaseURL.BASE_URL + BaseURL.LOAD_MATCHING + "/{loadMatchId}/request")
    public ResponseEntity<?> requestMatch(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long loadMatchId) {
        log.info("API: Request Load Match, ID: {}", loadMatchId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = loadMatchingService.requestMatch(loadMatchId, user.getStaffId());
        } catch (Exception e) {
            log.error("Error requesting load match ID {}: {}", loadMatchId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).LOADMATCH_ACCEPT)")
    @PostMapping(BaseURL.BASE_URL + BaseURL.LOAD_MATCHING + "/{loadMatchId}/accept")
    public ResponseEntity<?> acceptMatch(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long loadMatchId) {
        log.info("API: Accept Load Match, ID: {}", loadMatchId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = loadMatchingService.acceptMatch(loadMatchId, user.getStaffId());
        } catch (Exception e) {
            log.error("Error accepting load match ID {}: {}", loadMatchId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).LOADMATCH_REJECT)")
    @PostMapping(BaseURL.BASE_URL + BaseURL.LOAD_MATCHING + "/{loadMatchId}/reject")
    public ResponseEntity<?> rejectMatch(@AuthenticationPrincipal LoggedInUser user,
                                          @PathVariable Long loadMatchId,
                                          @RequestBody(required = false) Map<String, String> body) {
        log.info("API: Reject Load Match, ID: {}", loadMatchId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            String reason = body != null ? body.get("reason") : null;
            response = loadMatchingService.rejectMatch(loadMatchId, user.getStaffId(), reason);
        } catch (Exception e) {
            log.error("Error rejecting load match ID {}: {}", loadMatchId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
