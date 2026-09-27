package com.cargosetu.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.dto.TripDTO;
import com.cargosetu.service.TripService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.utils.PermissionConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(BaseURL.BASE_URL + UrlConstants.TripUrlConstants.TRIP)
@Slf4j
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_CREATE)")
    @PostMapping
    public ResponseEntity<?> createTrip(@AuthenticationPrincipal LoggedInUser user,@RequestBody TripDTO tripDTO) {
        log.info("API: Create Trip");
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripService.createTrip(user,tripDTO);
        } catch (Exception e) {
            log.error("Error creating trip: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_READ)")
    @GetMapping("/{tripId}")
    public ResponseEntity<?> getTrip(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long tripId) {
        log.info("API: Get Trip with ID: {}", tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripService.getTrip(user, tripId);
        } catch (Exception e) {
            log.error("Error getting trip with ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_READ)")
    @GetMapping
    public ResponseEntity<?> getAllTrips(@AuthenticationPrincipal LoggedInUser user) {
        log.info("API: Get All Trips");
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripService.getAllTrips(user);
        } catch (Exception e) {
            log.error("Error fetching all trips: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_UPDATE)")
    @PutMapping("/{tripId}")
    public ResponseEntity<?> updateTrip(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long tripId, @RequestBody TripDTO tripDTO) {
        log.info("API: Update Trip with ID: {}", tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripService.updateTrip(user,tripId, tripDTO);
        } catch (Exception e) {
            log.error("Error updating trip with ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_DELETE)")
    @DeleteMapping("/{tripId}")
    public ResponseEntity<?> deleteTrip(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long tripId) {
        log.info("API: Delete Trip with ID: {}", tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripService.deleteTrip(user, tripId);
        } catch (Exception e) {
            log.error("Error deleting trip with ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
