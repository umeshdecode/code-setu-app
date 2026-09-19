package com.cargosetu.loadmatch.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.loadmatch.dtos.ShipmentDTO;
import com.cargosetu.loadmatch.dtos.ShipmentItemDTO;
import com.cargosetu.loadmatch.service.ShipmentService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(BaseURL.BASE_URL + BaseURL.SHIPMENT)
@Slf4j
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).SHIPMENT_CREATE)")
    @PostMapping
    public ResponseEntity<?> createShipment(@AuthenticationPrincipal LoggedInUser user, @RequestBody ShipmentDTO shipmentDTO) {
        log.info("API: Create Shipment");
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = shipmentService.createShipment(user, shipmentDTO);
        } catch (Exception e) {
            log.error("Error creating shipment: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).SHIPMENT_READ)")
    @GetMapping("/{shipmentId}")
    public ResponseEntity<?> getShipment(@PathVariable Long shipmentId) {
        log.info("API: Get Shipment with ID: {}", shipmentId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = shipmentService.getShipment(shipmentId);
        } catch (Exception e) {
            log.error("Error getting shipment with ID {}: {}", shipmentId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).SHIPMENT_READ)")
    @GetMapping
    public ResponseEntity<?> getShipments(@AuthenticationPrincipal LoggedInUser user,
                                           @RequestParam(defaultValue = "0") Integer pageNumber,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("API: Get Shipments, page={}, size={}", pageNumber, pageSize);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = shipmentService.getShipments(user, pageNumber, pageSize);
        } catch (Exception e) {
            log.error("Error fetching shipments: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).SHIPMENT_UPDATE)")
    @PutMapping("/{shipmentId}")
    public ResponseEntity<?> updateShipment(@PathVariable Long shipmentId, @RequestBody ShipmentDTO shipmentDTO) {
        log.info("API: Update Shipment with ID: {}", shipmentId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = shipmentService.updateShipment(shipmentId, shipmentDTO);
        } catch (Exception e) {
            log.error("Error updating shipment with ID {}: {}", shipmentId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).SHIPMENT_DELETE)")
    @DeleteMapping("/{shipmentId}")
    public ResponseEntity<?> deleteShipment(@PathVariable Long shipmentId) {
        log.info("API: Delete Shipment with ID: {}", shipmentId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = shipmentService.deleteShipment(shipmentId);
        } catch (Exception e) {
            log.error("Error deleting shipment with ID {}: {}", shipmentId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).SHIPMENT_UPDATE)")
    @PostMapping("/{shipmentId}/items")
    public ResponseEntity<?> addItem(@PathVariable Long shipmentId, @RequestBody ShipmentItemDTO itemDTO) {
        log.info("API: Add Item to Shipment ID: {}", shipmentId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = shipmentService.addItem(shipmentId, itemDTO);
        } catch (Exception e) {
            log.error("Error adding item to shipment ID {}: {}", shipmentId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).SHIPMENT_UPDATE)")
    @PutMapping("/{shipmentId}/items/{itemId}")
    public ResponseEntity<?> updateItem(@PathVariable Long shipmentId, @PathVariable Long itemId, @RequestBody ShipmentItemDTO itemDTO) {
        log.info("API: Update Item {} for Shipment ID: {}", itemId, shipmentId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = shipmentService.updateItem(shipmentId, itemId, itemDTO);
        } catch (Exception e) {
            log.error("Error updating item {} for shipment ID {}: {}", itemId, shipmentId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).SHIPMENT_UPDATE)")
    @DeleteMapping("/{shipmentId}/items/{itemId}")
    public ResponseEntity<?> deleteItem(@PathVariable Long shipmentId, @PathVariable Long itemId) {
        log.info("API: Delete Item {} for Shipment ID: {}", itemId, shipmentId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = shipmentService.deleteItem(shipmentId, itemId);
        } catch (Exception e) {
            log.error("Error deleting item {} for shipment ID {}: {}", itemId, shipmentId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
