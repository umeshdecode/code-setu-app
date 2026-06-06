package com.cargosetu.shifting.controller;

import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.shifting.dtos.ShiftInventoryItemDTO;
import com.cargosetu.shifting.service.ShiftInventoryService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.utils.PermissionConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(BaseURL.BASE_URL + UrlConstants.ShiftingUrlConstants.SHIFT_INVENTORY)
@Slf4j
public class ShiftInventoryController {

    private final ShiftInventoryService shiftInventoryService;

    public ShiftInventoryController(ShiftInventoryService shiftInventoryService) {
        this.shiftInventoryService = shiftInventoryService;
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).SHIFT_INVENTORY_CREATE)")
    @PostMapping
    public ResponseEntity<?> createShiftInventoryItem(@RequestBody ShiftInventoryItemDTO itemDTO) {
        log.info("API: Create Shift Inventory Item");
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = shiftInventoryService.createShiftInventoryItem(itemDTO);
        } catch (Exception e) {
            log.error("Error creating shift inventory item: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).SHIFT_INVENTORY_READ)")
    @GetMapping("/{itemId}")
    public ResponseEntity<?> getShiftInventoryItem(@PathVariable Long itemId) {
        log.info("API: Get Shift Inventory Item with ID: {}", itemId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = shiftInventoryService.getShiftInventoryItem(itemId);
        } catch (Exception e) {
            log.error("Error getting shift inventory item with ID {}: {}", itemId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).SHIFT_INVENTORY_UPDATE)")
    @PutMapping("/{itemId}")
    public ResponseEntity<?> updateShiftInventoryItem(@PathVariable Long itemId, @RequestBody ShiftInventoryItemDTO itemDTO) {
        log.info("API: Update Shift Inventory Item with ID: {}", itemId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = shiftInventoryService.updateShiftInventoryItem(itemId, itemDTO);
        } catch (Exception e) {
            log.error("Error updating shift inventory item with ID {}: {}", itemId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).SHIFT_INVENTORY_STATUS_UPDATE)")
    @PatchMapping("/{itemId}/status")
    public ResponseEntity<?> updateShiftInventoryItemStatus(
            @PathVariable Long itemId,
            @RequestParam(required = false) Boolean packed,
            @RequestParam(required = false) Boolean loaded,
            @RequestParam(required = false) Boolean unloaded) {
        log.info("API: Update Shift Inventory Item Status for ID: {}", itemId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = shiftInventoryService.updateItemStatus(itemId, packed, loaded, unloaded);
        } catch (Exception e) {
            log.error("Error updating status for shift inventory item with ID {}: {}", itemId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
