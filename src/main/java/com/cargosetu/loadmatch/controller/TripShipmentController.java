package com.cargosetu.loadmatch.controller;

import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.loadmatch.dtos.TripShipmentDTO;
import com.cargosetu.loadmatch.service.TripShipmentService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Phase 3: read/update endpoints for a Trip's manifest (TripShipment rows) -
 * the shipments actually allocated onto a trip once a LoadMatch is accepted.
 */
@RestController
@Slf4j
public class TripShipmentController {

    private final TripShipmentService tripShipmentService;

    public TripShipmentController(TripShipmentService tripShipmentService) {
        this.tripShipmentService = tripShipmentService;
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).MANIFEST_READ)")
    @GetMapping(BaseURL.BASE_URL + UrlConstants.TripUrlConstants.TRIP + "/{tripId}/manifest")
    public ResponseEntity<?> getManifest(@PathVariable Long tripId) {
        log.info("API: Get Manifest for Trip ID: {}", tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripShipmentService.getManifestForTrip(tripId);
        } catch (Exception e) {
            log.error("Error fetching manifest for trip ID {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).MANIFEST_UPDATE)")
    @PutMapping(BaseURL.BASE_URL + UrlConstants.TripUrlConstants.TRIP + "/{tripId}/manifest/{tripShipmentId}")
    public ResponseEntity<?> updateManifestEntry(@PathVariable Long tripId,
                                                  @PathVariable Long tripShipmentId,
                                                  @RequestBody TripShipmentDTO updateDTO) {
        log.info("API: Update Manifest Entry {} for Trip ID: {}", tripShipmentId, tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = tripShipmentService.updateManifestEntry(tripId, tripShipmentId, updateDTO);
        } catch (Exception e) {
            log.error("Error updating manifest entry {} for trip ID {}: {}", tripShipmentId, tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
