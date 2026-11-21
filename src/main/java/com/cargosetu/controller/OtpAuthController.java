package com.cargosetu.controller;

import com.cargosetu.config.dto.TokenResponse;
import com.cargosetu.service.OtpService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth/otp")
@RequiredArgsConstructor
public class OtpAuthController {

    private final OtpService otpService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody OtpSendRequest request) {
        log.info("REST request to send OTP to: {}", request.getMobileNumber());
        Map<String, Object> response = otpService.sendOtp(request.getMobileNumber());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<TokenResponse> verifyOtp(@RequestBody OtpVerifyRequest request) {
        log.info("REST request to verify OTP for: {}", request.getMobileNumber());
        TokenResponse response = otpService.verifyOtpAndLogin(request.getMobileNumber(), request.getOtpCode());
        return ResponseEntity.ok(response);
    }

    @Data
    public static class OtpSendRequest {
        private String mobileNumber;
    }

    @Data
    public static class OtpVerifyRequest {
        private String mobileNumber;
        private String otpCode;
    }
}
