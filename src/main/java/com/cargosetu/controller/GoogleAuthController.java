package com.cargosetu.controller;

import com.cargosetu.config.dto.TokenResponse;
import com.cargosetu.service.GoogleAuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth/google")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody GoogleLoginRequest request) {
        log.info("REST request to log in with Google ID token");
        TokenResponse response = googleAuthService.loginWithGoogle(request.getIdToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(@RequestBody GoogleLoginRequest request) {
        log.info("REST request to sign up with Google ID token");
        TokenResponse response = googleAuthService.signupWithGoogle(request.getIdToken());
        return ResponseEntity.ok(response);
    }

    @Data
    public static class GoogleLoginRequest {
        private String idToken;
    }
}
