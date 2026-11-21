package com.cargosetu.marketplace.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants.MarketplaceUrlConstants;
import com.cargosetu.marketplace.dto.*;
import com.cargosetu.marketplace.service.DisputeService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.utils.PermissionConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(BaseURL.BASE_URL + MarketplaceUrlConstants.MARKETPLACE + MarketplaceUrlConstants.DISPUTES)
public class MarketplaceDisputeController {

    private final DisputeService disputeService;

    @PostMapping("/booking/{bookingId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_DISPUTE_CREATE + "')")
    public ResponseEntity<?> raiseDispute(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long bookingId, @RequestBody DisputeRequest request) {
        log.info("API entry for raiseDispute bookingId: {}", bookingId);
        try {
            GenericDataDTO response = disputeService.raiseDispute(user, bookingId, request);
            return ResponseEntity.status(CommonStatusCode.CREATED).body(response);
        } catch (Exception e) {
            log.error("Exception in raiseDispute", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to raise dispute", null, new Object[]{e.getMessage()}));
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_DISPUTE_READ + "')")
    public ResponseEntity<?> getMyDisputes(@AuthenticationPrincipal LoggedInUser user) {
        log.info("API entry for getMyDisputes user: {}", user.getStaffId());
        try {
            GenericDataDTO response = disputeService.getMyDisputes(user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in getMyDisputes", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to get disputes", null, new Object[]{e.getMessage()}));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_DISPUTE_READ + "')")
    public ResponseEntity<?> getDispute(@PathVariable Long id) {
        log.info("API entry for getDispute id: {}", id);
        try {
            GenericDataDTO response = disputeService.getDispute(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in getDispute", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to get dispute", null, new Object[]{e.getMessage()}));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_DISPUTE_UPDATE + "')")
    public ResponseEntity<?> updateDispute(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id, @RequestBody DisputeUpdateRequest request) {
        log.info("API entry for updateDispute id: {}", id);
        try {
            GenericDataDTO response = disputeService.updateDispute(user, id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in updateDispute", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to update dispute", null, new Object[]{e.getMessage()}));
        }
    }

    @PostMapping("/{id}/evidence")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_DISPUTE_UPDATE + "')")
    public ResponseEntity<?> addEvidence(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id, @RequestParam String evidenceType, @RequestParam String fileRef, @RequestParam(required = false) String description) {
        log.info("API entry for addEvidence id: {}", id);
        try {
            GenericDataDTO response = disputeService.addEvidence(user, id, evidenceType, fileRef, description);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in addEvidence", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to add evidence", null, new Object[]{e.getMessage()}));
        }
    }
}
