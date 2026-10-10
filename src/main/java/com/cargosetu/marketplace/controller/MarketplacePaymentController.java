package com.cargosetu.marketplace.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants.MarketplaceUrlConstants;
import com.cargosetu.marketplace.dto.*;
import com.cargosetu.marketplace.service.PaymentService;
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

import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(BaseURL.BASE_URL + MarketplaceUrlConstants.MARKETPLACE)
public class MarketplacePaymentController {

    private final PaymentService paymentService;

    @GetMapping(MarketplaceUrlConstants.BOOKINGS + "/{bookingId}" + MarketplaceUrlConstants.PAYMENTS)
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_PAYMENT_READ + "')")
    public ResponseEntity<?> getPaymentHistory(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long bookingId) {
        log.info("API entry for getPaymentHistory bookingId: {}", bookingId);
        try {
            GenericDataDTO response = paymentService.getPaymentHistory(bookingId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in getPaymentHistory", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to get payment history", null, new Object[]{e.getMessage()}));
        }
    }

    @PostMapping(MarketplaceUrlConstants.BOOKINGS + "/{bookingId}" + MarketplaceUrlConstants.PAYMENTS)
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_PAYMENT_CREATE + "')")
    public ResponseEntity<?> createPayment(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long bookingId, @RequestBody CreatePaymentOrderRequest request) {
        log.info("API entry for createPayment bookingId: {}", bookingId);
        try {
            GenericDataDTO response = paymentService.createPaymentOrder(user, bookingId, request);
            return ResponseEntity.status(CommonStatusCode.CREATED).body(response);
        } catch (Exception e) {
            log.error("Exception in createPayment", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to create payment", null, new Object[]{e.getMessage()}));
        }
    }

    @PostMapping(MarketplaceUrlConstants.PAYMENTS + "/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> payload) {
        log.info("API entry for handleWebhook");
        try {
            GenericDataDTO response = paymentService.handleWebhook(payload);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in handleWebhook", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to handle webhook", null, new Object[]{e.getMessage()}));
        }
    }

    @GetMapping(MarketplaceUrlConstants.WALLET)
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_WALLET_READ + "')")
    public ResponseEntity<?> getWalletStatement(@AuthenticationPrincipal LoggedInUser user) {
        log.info("API entry for getWalletStatement user: {}", user.getStaffId());
        try {
            GenericDataDTO response = paymentService.getWalletStatement(user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in getWalletStatement", e);
            return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                    .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR, "Failed to get wallet statement", null, new Object[]{e.getMessage()}));
        }
    }
}
