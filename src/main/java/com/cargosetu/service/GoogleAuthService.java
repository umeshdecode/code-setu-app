package com.cargosetu.service;

import com.cargosetu.config.JwtUtils;
import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.dto.TokenResponse;
import com.cargosetu.customer.entity.Customer;
import com.cargosetu.customer.repository.CustomerRepository;
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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Verifies "Sign in with Google" ID tokens and issues our own JWT so the
 * frontend keeps a single token format regardless of login method.
 *
 * Staff accounts must already exist (matched by email) - Google login does not
 * provision staff. Customers are auto-created on first Google sign-in.
 */
@Slf4j
@Service
public class GoogleAuthService {

    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;
    private final JwtUtils jwtUtils;
    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthService(StaffRepository staffRepository,
                              CustomerRepository customerRepository,
                              JwtUtils jwtUtils,
                              @Value("${app.auth.google.client-id:}") String googleClientId) throws GeneralSecurityException, IOException {
        this.staffRepository = staffRepository;
        this.customerRepository = customerRepository;
        this.jwtUtils = jwtUtils;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    @Transactional
    public TokenResponse loginWithGoogle(String idTokenString) {
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

        String email = payload.getEmail();
        String firstName = (String) payload.get("given_name");
        String lastName = (String) payload.get("family_name");

        // Staff must be pre-provisioned by an admin - no auto-create.
        Optional<Staff> staffOpt = staffRepository.findByEmail(email);
        if (staffOpt.isPresent()) {
            return issueStaffToken(staffOpt.get());
        }

        // Customers can self-register via Google.
        Customer customer = customerRepository.findByEmailAndIsDelete(email, false)
                .orElseGet(() -> createCustomer(email, firstName, lastName));

        return issueCustomerToken(customer);
    }

    private Customer createCustomer(String email, String firstName, String lastName) {
        log.info("Auto-creating customer for first-time Google sign-in: {}", email);
        Customer customer = new Customer();
        customer.setUuid(UUID.randomUUID().toString());
        customer.setEmail(email);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setIsDelete(false);
        return customerRepository.save(customer);
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

    private TokenResponse issueCustomerToken(Customer customer) {
        LoggedInUser loggedInUser = new LoggedInUser();
        loggedInUser.setStaffId(customer.getCustomerId());
        loggedInUser.setUuid(customer.getUuid());
        loggedInUser.setUserName(customer.getEmail());
        loggedInUser.setFirstName(customer.getFirstName());
        loggedInUser.setLastName(customer.getLastName());
        loggedInUser.setEmail(customer.getEmail());
        loggedInUser.setCompanyId(customer.getCompanyId());
        loggedInUser.setRoles(Set.of("CUSTOMER"));
        loggedInUser.setPermissions(new HashSet<>());

        String token = jwtUtils.generateToken(loggedInUser);
        return new TokenResponse(token, jwtUtils.getTokenExpirationTime());
    }
}
