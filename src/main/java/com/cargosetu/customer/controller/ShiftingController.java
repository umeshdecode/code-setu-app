package com.cargosetu.customer.controller;

import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.customer.dtos.ShiftingDTO;
import com.cargosetu.customer.service.ShiftingService; // Assuming this service exists
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.utils.PermissionConstants; // Assuming this exists for PreAuthorize
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(BaseURL.BASE_URL + UrlConstants.CustomerUrlConstants.SHIFTS)
@Slf4j
public class ShiftingController {

    private final ShiftingService shiftingService;

    public ShiftingController(ShiftingService shiftingService) {
        this.shiftingService = shiftingService;
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).SHIFT_CREATE)")
    @PostMapping
    public ResponseEntity<?> createShifting(@RequestBody ShiftingDTO shiftingDTO) {
        log.info("API: Create Shifting");
        GenericDataDTO response = new GenericDataDTO();
        try {
            // Assuming ShiftingService has a method to validate and create
            response = shiftingService.createShifting(shiftingDTO);
        } catch (Exception e) {
            log.error("Error creating shifting: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).SHIFT_READ)")
    @GetMapping("/{shiftId}")
    public ResponseEntity<?> getShifting(@PathVariable Long shiftId) {
        log.info("API: Get Shifting with ID: {}", shiftId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            // Assuming ShiftingService has a method to get a shifting by ID
            response = shiftingService.getShifting(shiftId);
        } catch (Exception e) {
            log.error("Error getting shifting with ID {}: {}", shiftId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).SHIFT_UPDATE)")
    @PutMapping("/{shiftId}")
    public ResponseEntity<?> updateShifting(@PathVariable Long shiftId, @RequestBody ShiftingDTO shiftingDTO) {
        log.info("API: Update Shifting with ID: {}", shiftId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            // Ensure the ID from the path is used for the update
            shiftingDTO.setShiftingId(shiftId);
            // Assuming ShiftingService has a method to validate and update
            response = shiftingService.updateShifting(shiftingDTO);
        } catch (Exception e) {
            log.error("Error updating shifting with ID {}: {}", shiftId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shifting-types")
    @PreAuthorize(
            "hasAuthority(T(com.cargosetu.utils.PermissionConstants).CUSTOMER_VIEW)"
    )
    public ResponseEntity<?> getShiftingTypes() {
        GenericDataDTO response = new GenericDataDTO();
        try {
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setData(List.of("House Shifting","Office Shifting",
                    "Factory Shifting","Mall Shifting","Shop Shifting"
                    ,"Warehouse Shifting","Goods Shifting",
                    "Govt. Related Shifting",
                    "Medical Item Shifting","Electronic Item Shift","Liquid Items Shift",
                    "Industrial Items Shift","Others"));
            response.setResponseMessege("Success");
        } catch (Exception e) {
            log.error("Error {} ", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
