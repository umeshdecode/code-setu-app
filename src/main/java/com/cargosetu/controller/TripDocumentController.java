package com.cargosetu.controller;

import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.service.TripDocumentService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.utils.PermissionConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(BaseURL.BASE_URL + UrlConstants.TripUrlConstants.TRIP_DOCUMENT)
@Slf4j
public class TripDocumentController {

    private final TripDocumentService service;

    public TripDocumentController(TripDocumentService service) {
        this.service = service;
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_DOCUMENT_CREATE)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tripId") Long tripId,
            @RequestParam(value = "documentType", required = false) String documentType) {
        log.info("API: Upload Trip Document");
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.create(file, tripId, documentType);
        } catch (Exception e) {
            log.error("Error uploading trip document: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_DOCUMENT_READ)")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        log.info("API: Get Trip Document by ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.getById(id);
        } catch (Exception e) {
            log.error("Error fetching trip document {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_DOCUMENT_READ)")
    @GetMapping
    public ResponseEntity<?> getAll() {
        log.info("API: Get All Trip Documents");
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.getAll();
        } catch (Exception e) {
            log.error("Error fetching all trip documents: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_DOCUMENT_READ)")
    @GetMapping("/by-trip/{tripId}")
    public ResponseEntity<?> getByTripId(@PathVariable Long tripId) {
        log.info("API: Get Trip Documents by Trip ID: {}", tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.getByTripId(tripId);
        } catch (Exception e) {
            log.error("Error fetching documents for trip {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_DOCUMENT_UPDATE)")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "tripId", required = false) Long tripId,
            @RequestParam(value = "documentType", required = false) String documentType) {
        log.info("API: Update Trip Document with ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.update(id, file, tripId, documentType);
        } catch (Exception e) {
            log.error("Error updating trip document {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_DOCUMENT_DELETE)")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        log.info("API: Delete Trip Document with ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.delete(id);
        } catch (Exception e) {
            log.error("Error deleting trip document {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
