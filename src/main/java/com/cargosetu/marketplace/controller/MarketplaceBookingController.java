package com.cargosetu.marketplace.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants.MarketplaceUrlConstants;
import com.cargosetu.marketplace.dto.*;
import com.cargosetu.marketplace.service.BookingService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.marketplace.util.MarketplaceResponses;
import com.cargosetu.utils.PermissionConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(BaseURL.BASE_URL + MarketplaceUrlConstants.MARKETPLACE + MarketplaceUrlConstants.BOOKINGS)
public class MarketplaceBookingController {

    private final BookingService bookingService;

    @PostMapping("/listing/{listingId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_CREATE + "')")
    public ResponseEntity<?> createBooking(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long listingId, @Valid @RequestBody CreateBookingRequest request) {
        log.info("API entry for createBooking listingId: {}", listingId);
        try {
            GenericDataDTO response = bookingService.createBooking(user, listingId, request);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in createBooking", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_CREATE + "')")
    public ResponseEntity<?> createBookingDirect(@AuthenticationPrincipal LoggedInUser user, @Valid @RequestBody CreateBookingRequest request) {
        Long listingId = request != null ? request.getListingId() : null;
        log.info("API entry for createBookingDirect listingId: {}", listingId);
        if (listingId == null) {
            return ResponseEntity.badRequest().body(new GenericDataDTO(CommonStatusCode.FAIL, "listingId is required", null, null));
        }
        try {
            GenericDataDTO response = bookingService.createBooking(user, listingId, request);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in createBookingDirect", e);
            return MarketplaceResponses.error();
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_READ + "')")
    public ResponseEntity<?> getMyBookings(
            @AuthenticationPrincipal LoggedInUser user,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role) {
        log.info("API entry for getMyBookings user: {}, status: {}, role: {}", user != null ? user.getStaffId() : null, status, role);
        try {
            GenericDataDTO response = bookingService.getMyBookings(user, status, role);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in getMyBookings", e);
            return MarketplaceResponses.error();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_READ + "')")
    public ResponseEntity<?> getBooking(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        log.info("API entry for getBooking id: {}", id);
        try {
            GenericDataDTO response = bookingService.getBooking(user, id);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in getBooking", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_CREATE + "')")
    public ResponseEntity<?> acceptBooking(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id,
                                           @RequestBody(required = false) AssignVehicleDriverRequest assignment,
                                           @RequestParam(required = false) Long vehicleId,
                                           @RequestParam(required = false) Long driverId) {
        log.info("API entry for acceptBooking id: {}", id);
        try {
            // Older app builds send vehicleId/driverId as query params; newer ones send a JSON body.
            if (assignment == null && (vehicleId != null || driverId != null)) {
                assignment = new AssignVehicleDriverRequest();
                assignment.setVehicleId(vehicleId);
                assignment.setDriverId(driverId);
            }
            GenericDataDTO response = bookingService.acceptBooking(user, id, assignment);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in acceptBooking", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_CREATE + "')")
    public ResponseEntity<?> rejectBooking(
            @AuthenticationPrincipal LoggedInUser user,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RejectBookingRequest request) {
        log.info("API entry for rejectBooking id: {}", id);
        try {
            GenericDataDTO response = bookingService.rejectBooking(user, id, request);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in rejectBooking", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping("/{id}/counter")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_CREATE + "')")
    public ResponseEntity<?> counterBooking(
            @AuthenticationPrincipal LoggedInUser user,
            @PathVariable Long id,
            @Valid @RequestBody CounterBookingRequest request) {
        log.info("API entry for counterBooking id: {}", id);
        try {
            GenericDataDTO response = bookingService.counterBooking(user, id, request);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in counterBooking", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_CANCEL + "')")
    public ResponseEntity<?> cancelBooking(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id, @Valid @RequestBody(required = false) CancellationRequest request) {
        log.info("API entry for cancelBooking id: {}", id);
        try {
            GenericDataDTO response = bookingService.cancelBooking(user, id, request);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in cancelBooking", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_ASSIGN + "')")
    public ResponseEntity<?> assignVehicleDriver(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id, @Valid @RequestBody AssignVehicleDriverRequest request) {
        log.info("API entry for assignVehicleDriver id: {}", id);
        try {
            GenericDataDTO response = bookingService.assignVehicleDriver(user, id, request);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in assignVehicleDriver", e);
            return MarketplaceResponses.error();
        }
    }
}
