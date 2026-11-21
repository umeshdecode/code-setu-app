package com.cargosetu.service;

import com.cargosetu.config.JwtUtils;
import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.dto.TokenResponse;
import com.cargosetu.dto.StaffDTO;
import com.cargosetu.handler.CustomValidationException;
import com.cargosetu.model.Permission;
import com.cargosetu.model.Staff;
import com.cargosetu.repo.StaffRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.GeneralSecurityException;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Verifies "Sign in with Google" ID tokens and issues our own JWT so the
 * frontend keeps a single token format regardless of login method.
 *
 * Google is a Staff-only auth method here, mirroring email/password:
 * loginWithGoogle only authenticates an existing Staff, signupWithGoogle
 * only creates a new one (via the same StaffService.saveStaff used by the
 * manual /auth/register flow, so defaults - COMPANY_ADMIN role, uuid,
 * username, hashed password - stay identical).
 */
@Slf4j
@Service
public class GoogleAuthService {

    private final StaffRepository staffRepository;
    private final StaffService staffService;
    private final JwtUtils jwtUtils;
    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthService(StaffRepository staffRepository,
                              StaffService staffService,
                              JwtUtils jwtUtils,
                              @Value("${app.auth.google.client-id:}") String googleClientId) throws GeneralSecurityException, IOException {
        this.staffRepository = staffRepository;
        this.staffService = staffService;
        this.jwtUtils = jwtUtils;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    @Transactional
    public TokenResponse loginWithGoogle(String idTokenString) {
        GoogleIdToken.Payload payload = verifyToken(idTokenString);
        String email = payload.getEmail();

        Staff staff = staffRepository.findByEmail(email)
                .orElseThrow(() -> new CustomValidationException(HttpStatus.UNAUTHORIZED.value(),
                        "No account found for this Google email. Please sign up first."));

        return issueStaffToken(staff);
    }

    @Transactional
    public TokenResponse signupWithGoogle(String idTokenString) {
        GoogleIdToken.Payload payload = verifyToken(idTokenString);
        String email = payload.getEmail();

        if (staffRepository.findByEmail(email).isPresent()) {
            throw new CustomValidationException(HttpStatus.BAD_REQUEST.value(),
                    "An account with this email already exists. Please log in instead.");
        }

        StaffDTO staffDTO = new StaffDTO();
        staffDTO.setFirstName((String) payload.get("given_name"));
        staffDTO.setLastName((String) payload.get("family_name"));
        staffDTO.setEmail(email);
        staffDTO.setPassword(UUID.randomUUID().toString());

        staffService.saveStaff(staffDTO, true);

        Staff staff = staffRepository.findByEmail(email)
                .orElseThrow(() -> new CustomValidationException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Failed to create account: record disappeared!"));

        return issueStaffToken(staff);
    }

    private GoogleIdToken.Payload verifyToken(String idTokenString) {
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new CustomValidationException(HttpStatus.BAD_REQUEST.value(), "Google ID token is required!");
        }

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException e) {
            log.error("Failed to verify Google ID token", e);
            throw new CustomValidationException(HttpStatus.UNAUTHORIZED.value(), "Failed to verify Google token!");
        }

        if (idToken == null) {
            throw new CustomValidationException(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired Google token!");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new CustomValidationException(HttpStatus.UNAUTHORIZED.value(), "Google account email is not verified!");
        }

        return payload;
    }

    private TokenResponse issueStaffToken(Staff staff) {
        Set<String> roles = staff.getRoles().stream().map(role -> role.getCode()).collect(Collectors.toSet());
        Set<String> permissions = staff.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        LoggedInUser loggedInUser = new LoggedInUser();
        loggedInUser.setStaffId(staff.getStaffId());
        loggedInUser.setUuid(staff.getUuid());
        loggedInUser.setUserName(staff.getUserName());
        loggedInUser.setFirstName(staff.getFirstName());
        loggedInUser.setLastName(staff.getLastName());
        loggedInUser.setEmail(staff.getEmail());
        loggedInUser.setCompanyId(staff.getCompanyId());
        loggedInUser.setRoles(roles);
        loggedInUser.setPermissions(permissions);

        String token = jwtUtils.generateToken(loggedInUser);
        return new TokenResponse(token, jwtUtils.getTokenExpirationTime());
    }
}
