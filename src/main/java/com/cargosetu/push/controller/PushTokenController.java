package com.cargosetu.push.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.push.dto.PushTokenRegisterDTO;
import com.cargosetu.push.service.PushTokenService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Self-service device/token lifecycle for the authenticated account. Any
 * logged-in Staff or Customer can register, list and remove their own
 * installations - there is no separate permission to check (this is not
 * admin-scoped data), and the owner always comes from the principal, never
 * from the request body (plan §10 "never trust a client-supplied user_id").
 */
@RestController
@RequestMapping(BaseURL.BASE_URL + "/push-tokens")
@Slf4j
public class PushTokenController {

    private final PushTokenService pushTokenService;

    public PushTokenController(PushTokenService pushTokenService) {
        this.pushTokenService = pushTokenService;
    }

    @PostMapping
    public ResponseEntity<?> register(@AuthenticationPrincipal LoggedInUser user,
                                      @Valid @RequestBody PushTokenRegisterDTO request) {
        log.info("API: Register push token, deviceId={}", request.getDeviceId());
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = pushTokenService.register(user, request);
        } catch (Exception e) {
            log.error("Error registering push token: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> listMine(@AuthenticationPrincipal LoggedInUser user) {
        log.info("API: List my push tokens");
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = pushTokenService.listMine(user);
        } catch (Exception e) {
            log.error("Error listing push tokens: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/current")
    public ResponseEntity<?> deactivateCurrent(@AuthenticationPrincipal LoggedInUser user,
                                               @RequestParam String deviceId) {
        log.info("API: Unregister current push device, deviceId={}", deviceId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = pushTokenService.deactivateCurrent(user, deviceId);
        } catch (Exception e) {
            log.error("Error unregistering current push device: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivateById(@AuthenticationPrincipal LoggedInUser user, @PathVariable Long id) {
        log.info("API: Remove push device id={}", id);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = pushTokenService.deactivateById(user, id);
        } catch (Exception e) {
            log.error("Error removing push device id={}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
