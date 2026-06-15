package com.cargosetu.controller;

import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.dto.TripExpenseDTO;
import com.cargosetu.service.TripExpenseService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.utils.PermissionConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(BaseURL.BASE_URL + UrlConstants.TripUrlConstants.TRIP_EXPENSE)
@Slf4j
public class TripExpenseController {

    private final TripExpenseService service;

    public TripExpenseController(TripExpenseService service) {
        this.service = service;
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_EXPENSE_CREATE)")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody TripExpenseDTO dto) {
        log.info("API: Create Trip Expense");
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.create(dto);
        } catch (Exception e) {
            log.error("Error creating trip expense: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_EXPENSE_READ)")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        log.info("API: Get Trip Expense by ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.getById(id);
        } catch (Exception e) {
            log.error("Error fetching trip expense {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_EXPENSE_READ)")
    @GetMapping
    public ResponseEntity<?> getAll() {
        log.info("API: Get All Trip Expenses");
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.getAll();
        } catch (Exception e) {
            log.error("Error fetching all trip expenses: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_EXPENSE_READ)")
    @GetMapping("/by-trip/{tripId}")
    public ResponseEntity<?> getByTripId(@PathVariable Long tripId) {
        log.info("API: Get Trip Expenses by Trip ID: {}", tripId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.getByTripId(tripId);
        } catch (Exception e) {
            log.error("Error fetching expenses for trip {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_EXPENSE_UPDATE)")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody TripExpenseDTO dto) {
        log.info("API: Update Trip Expense with ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.update(id, dto);
        } catch (Exception e) {
            log.error("Error updating trip expense {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).TRIP_EXPENSE_DELETE)")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        log.info("API: Delete Trip Expense with ID: {}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = service.delete(id);
        } catch (Exception e) {
            log.error("Error deleting trip expense {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
