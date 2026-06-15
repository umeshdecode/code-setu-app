package com.cargosetu.controller;

import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.dto.TripStatusHistoryDTO;
import com.cargosetu.service.TripStatusHistoryService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.utils.PermissionConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(BaseURL.BASE_URL + UrlConstants.TripUrlConstants.TRIP_STATUS_HISTORY)
@Slf4j
public class TripStatusHistoryController {

    private final TripStatusHistoryService service;

    public TripStatusHistoryController(TripStatusHistoryService service) {
        this.service = service;
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_STATUS_HISTORY_CREATE)")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody TripStatusHistoryDTO dto) {
        log.info("API: Create Trip Status History");
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.create(dto);
        } catch (Exception e) {
            log.error("Error creating trip status history: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_STATUS_HISTORY_READ)")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        log.info("API: Get Trip Status History by ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.getById(id);
        } catch (Exception e) {
            log.error("Error fetching trip status history {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_STATUS_HISTORY_READ)")
    @GetMapping
    public ResponseEntity<?> getAll() {
        log.info("API: Get All Trip Status Histories");
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.getAll();
        } catch (Exception e) {
            log.error("Error fetching all trip status histories: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_STATUS_HISTORY_READ)")
    @GetMapping("/by-trip/{tripId}")
    public ResponseEntity<?> getByTripId(@PathVariable Long tripId) {
        log.info("API: Get Trip Status Histories by Trip ID: {}", tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.getByTripId(tripId);
        } catch (Exception e) {
            log.error("Error fetching status history for trip {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_STATUS_HISTORY_UPDATE)")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody TripStatusHistoryDTO dto) {
        log.info("API: Update Trip Status History with ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.update(id, dto);
        } catch (Exception e) {
            log.error("Error updating trip status history {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_STATUS_HISTORY_DELETE)")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        log.info("API: Delete Trip Status History with ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.delete(id);
        } catch (Exception e) {
            log.error("Error deleting trip status history {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
