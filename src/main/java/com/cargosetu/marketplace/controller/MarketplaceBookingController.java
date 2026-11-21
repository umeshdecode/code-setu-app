package com.cargosetu.marketplace.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants.MarketplaceUrlConstants;
import com.cargosetu.marketplace.dto.*;
import com.cargosetu.marketplace.service.BookingService;
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
@RequestMapping(BaseURL.BASE_URL + MarketplaceUrlConstants.MARKETPLACE + MarketplaceUrlConstants.BOOKINGS)
public class MarketplaceBookingController {

    private final BookingService bookingService;

    @PostMapping("/listing/{listingId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_CREATE + "')")
    public ResponseEntity<?> createBooking(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long listingId, @RequestBody CreateBookingRequest request) {
        log.info("API entry for createBooking listingId: {}", listingId);
        try {
            GenericDataDTO response = bookingService.createBooking(user, listingId, request);
            return ResponseEntity.status(CommonStatusCode.CREATED).body(response);
        } catch (Exception e) {
            log.error("Exception in createBooking", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to create booking", null, new Object[]{e.getMessage()}));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_CREATE + "')")
    public ResponseEntity<?> createBookingDirect(@AuthenticationPrincipal LoggedInUser user, @RequestBody CreateBookingRequest request) {
        Long listingId = request != null ? request.getListingId() : null;
        log.info("API entry for createBookingDirect listingId: {}", listingId);
        if (listingId == null) {
            return ResponseEntity.badRequest().body(new GenericDataDTO(CommonStatusCode.FAIL, "listingId is required", null, null));
        }
        try {
            GenericDataDTO response = bookingService.createBooking(user, listingId, request);
            return ResponseEntity.status(CommonStatusCode.CREATED).body(response);
        } catch (Exception e) {
            log.error("Exception in createBookingDirect", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to create booking", null, new Object[]{e.getMessage()}));
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
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in getMyBookings", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to get bookings", null, new Object[]{e.getMessage()}));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_READ + "')")
    public ResponseEntity<?> getBooking(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        log.info("API entry for getBooking id: {}", id);
        try {
            GenericDataDTO response = bookingService.getBooking(user, id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in getBooking", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to get booking", null, new Object[]{e.getMessage()}));
        }
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_CREATE + "')")
    public ResponseEntity<?> acceptBooking(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        log.info("API entry for acceptBooking id: {}", id);
        try {
            GenericDataDTO response = bookingService.acceptBooking(user, id);
            int status = response.getResponseCode() != null ? response.getResponseCode() : CommonStatusCode.SUCCESS;
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            log.error("Exception in acceptBooking", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to accept booking", null, new Object[]{e.getMessage()}));
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_CREATE + "')")
    public ResponseEntity<?> rejectBooking(
            @AuthenticationPrincipal LoggedInUser user,
            @PathVariable Long id,
            @RequestBody(required = false) RejectBookingRequest request) {
        log.info("API entry for rejectBooking id: {}", id);
        try {
            GenericDataDTO response = bookingService.rejectBooking(user, id, request);
            int status = response.getResponseCode() != null ? response.getResponseCode() : CommonStatusCode.SUCCESS;
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            log.error("Exception in rejectBooking", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to reject booking", null, new Object[]{e.getMessage()}));
        }
    }

    @PostMapping("/{id}/counter")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_CREATE + "')")
    public ResponseEntity<?> counterBooking(
            @AuthenticationPrincipal LoggedInUser user,
            @PathVariable Long id,
            @RequestBody CounterBookingRequest request) {
        log.info("API entry for counterBooking id: {}", id);
        try {
            GenericDataDTO response = bookingService.counterBooking(user, id, request);
            int status = response.getResponseCode() != null ? response.getResponseCode() : CommonStatusCode.SUCCESS;
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            log.error("Exception in counterBooking", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to counter booking", null, new Object[]{e.getMessage()}));
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_CANCEL + "')")
    public ResponseEntity<?> cancelBooking(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id, @RequestBody(required = false) CancellationRequest request) {
        log.info("API entry for cancelBooking id: {}", id);
        try {
            GenericDataDTO response = bookingService.cancelBooking(user, id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in cancelBooking", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to cancel booking", null, new Object[]{e.getMessage()}));
        }
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_BOOKING_ASSIGN + "')")
    public ResponseEntity<?> assignVehicleDriver(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id, @RequestBody AssignVehicleDriverRequest request) {
        log.info("API entry for assignVehicleDriver id: {}", id);
        try {
            GenericDataDTO response = bookingService.assignVehicleDriver(user, id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in assignVehicleDriver", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to assign vehicle/driver", null, new Object[]{e.getMessage()}));
        }
    }
}
