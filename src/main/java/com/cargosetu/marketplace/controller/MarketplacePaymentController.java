package com.cargosetu.marketplace.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants.MarketplaceUrlConstants;
import com.cargosetu.marketplace.dto.*;
import com.cargosetu.marketplace.service.PaymentService;
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
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in getPaymentHistory", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping(MarketplaceUrlConstants.BOOKINGS + "/{bookingId}" + MarketplaceUrlConstants.PAYMENTS)
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_PAYMENT_CREATE + "')")
    public ResponseEntity<?> createPayment(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long bookingId, @Valid @RequestBody CreatePaymentOrderRequest request) {
        log.info("API entry for createPayment bookingId: {}", bookingId);
        try {
            GenericDataDTO response = paymentService.createPaymentOrder(user, bookingId, request);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in createPayment", e);
            return MarketplaceResponses.error();
        }
    }

    @PostMapping(MarketplaceUrlConstants.PAYMENTS + "/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> payload) {
        log.info("API entry for handleWebhook");
        try {
            GenericDataDTO response = paymentService.handleWebhook(payload);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in handleWebhook", e);
            return MarketplaceResponses.error();
        }
    }

    @GetMapping(MarketplaceUrlConstants.WALLET)
    @PreAuthorize("hasAuthority('" + PermissionConstants.MARKETPLACE_WALLET_READ + "')")
    public ResponseEntity<?> getWalletStatement(@AuthenticationPrincipal LoggedInUser user) {
        log.info("API entry for getWalletStatement user: {}", user.getStaffId());
        try {
            GenericDataDTO response = paymentService.getWalletStatement(user);
            return MarketplaceResponses.of(response);
        } catch (Exception e) {
            log.error("Exception in getWalletStatement", e);
            return MarketplaceResponses.error();
        }
    }
}
