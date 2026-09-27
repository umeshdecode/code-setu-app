package com.cargosetu.maps.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.maps.dto.GeocodeRequestDTO;
import com.cargosetu.maps.dto.RouteLocationUpdateDTO;
import com.cargosetu.maps.service.TripRouteService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(BaseURL.BASE_URL + UrlConstants.TripUrlConstants.TRIP)
@Slf4j
public class TripRouteController {

    private final TripRouteService tripRouteService;

    public TripRouteController(TripRouteService tripRouteService) {
        this.tripRouteService = tripRouteService;
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_READ)")
    @GetMapping(UrlConstants.TripUrlConstants.TRIP_ROUTE)
    public ResponseEntity<?> getTripRoute(@AuthenticationPrincipal LoggedInUser user,
                                          @PathVariable Long tripId,
                                          @RequestParam(defaultValue = "false") boolean refresh) {
        log.info("API: Get planned route for Trip ID: {} (refresh={})", tripId, refresh);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripRouteService.getTripRoute(user, tripId, refresh);
        } catch (Exception e) {
            log.error("Error getting route for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while loading the trip route.");
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_UPDATE)")
    @PostMapping(UrlConstants.TripUrlConstants.TRIP_ROUTE_GEOCODE)
    public ResponseEntity<?> geocode(@AuthenticationPrincipal LoggedInUser user,
                                     @PathVariable Long tripId,
                                     @Valid @RequestBody GeocodeRequestDTO request) {
        log.info("API: Geocode location candidates for Trip ID: {}", tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripRouteService.geocode(user, tripId, request);
        } catch (Exception e) {
            log.error("Error geocoding for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while searching the location.");
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_UPDATE)")
    @PutMapping(UrlConstants.TripUrlConstants.TRIP_ROUTE_LOCATION)
    public ResponseEntity<?> confirmLocation(@AuthenticationPrincipal LoggedInUser user,
                                             @PathVariable Long tripId,
                                             @Valid @RequestBody RouteLocationUpdateDTO update) {
        log.info("API: Confirm {} location for Trip ID: {}", update.getEndpoint(), tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripRouteService.confirmLocation(user, tripId, update);
        } catch (Exception e) {
            log.error("Error confirming location for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while saving the location.");
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
