package com.cargosetu.loadmatch.controller;

import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.loadmatch.dtos.TripCapacityDTO;
import com.cargosetu.loadmatch.dtos.TripStopDTO;
import com.cargosetu.loadmatch.service.TripCapacityService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(BaseURL.BASE_URL + UrlConstants.TripUrlConstants.TRIP)
@Slf4j
public class TripCapacityController {

    private final TripCapacityService tripCapacityService;

    public TripCapacityController(TripCapacityService tripCapacityService) {
        this.tripCapacityService = tripCapacityService;
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_READ)")
    @GetMapping("/{tripId}/capacity")
    public ResponseEntity<?> getCapacity(@PathVariable Long tripId) {
        log.info("API: Get Capacity for Trip ID: {}", tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripCapacityService.getCapacity(tripId);
        } catch (Exception e) {
            log.error("Error getting capacity for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_CAPACITY_UPDATE)")
    @PostMapping("/{tripId}/capacity/initialize")
    public ResponseEntity<?> initializeCapacity(@PathVariable Long tripId) {
        log.info("API: Initialize Capacity for Trip ID: {}", tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripCapacityService.initializeCapacity(tripId);
        } catch (Exception e) {
            log.error("Error initializing capacity for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_CAPACITY_UPDATE)")
    @PutMapping("/{tripId}/capacity")
    public ResponseEntity<?> updateCapacity(@PathVariable Long tripId, @Valid @RequestBody TripCapacityDTO capacityDTO) {
        log.info("API: Update Capacity for Trip ID: {}", tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripCapacityService.updateCapacity(tripId, capacityDTO);
        } catch (Exception e) {
            log.error("Error updating capacity for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_READ)")
    @GetMapping("/{tripId}/stops")
    public ResponseEntity<?> getStops(@PathVariable Long tripId) {
        log.info("API: Get Stops for Trip ID: {}", tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripCapacityService.getStops(tripId);
        } catch (Exception e) {
            log.error("Error getting stops for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_CAPACITY_UPDATE)")
    @PostMapping("/{tripId}/stops")
    public ResponseEntity<?> addStop(@PathVariable Long tripId, @RequestBody TripStopDTO stopDTO) {
        log.info("API: Add Stop to Trip ID: {}", tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripCapacityService.addStop(tripId, stopDTO);
        } catch (Exception e) {
            log.error("Error adding stop to trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_CAPACITY_UPDATE)")
    @DeleteMapping("/{tripId}/stops/{stopId}")
    public ResponseEntity<?> removeStop(@PathVariable Long tripId, @PathVariable Long stopId) {
        log.info("API: Remove Stop {} from Trip ID: {}", stopId, tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripCapacityService.removeStop(tripId, stopId);
        } catch (Exception e) {
            log.error("Error removing stop {} from trip ID {}: {}", stopId, tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_CAPACITY_UPDATE)")
    @PutMapping("/{tripId}/stops/reorder")
    public ResponseEntity<?> reorderStops(@PathVariable Long tripId, @RequestBody List<Long> orderedStopIds) {
        log.info("API: Reorder Stops for Trip ID: {}", tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripCapacityService.reorderStops(tripId, orderedStopIds);
        } catch (Exception e) {
            log.error("Error reordering stops for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
