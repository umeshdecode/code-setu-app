package com.cargosetu.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.dto.TokenResponse;
import com.cargosetu.config.JwtUtils;
import com.cargosetu.customer.entity.Customer;
import com.cargosetu.customer.repository.CustomerRepository;
import com.cargosetu.handler.CustomValidationException;
import com.cargosetu.model.Permission;
import com.cargosetu.model.Staff;
import com.cargosetu.model.OtpVerification;
import com.cargosetu.notification.event.GenericNotificationEvent;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.repo.StaffRepository;
import com.cargosetu.repo.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;
    private final OtpVerificationRepository otpRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final JwtUtils jwtUtils;

    @Value("${app.auth.otp.mock:true}")
    private boolean mockOtpEnabled;

    private final SecureRandom random = new SecureRandom();

    /**
     * Sends a mock/real OTP to the registered mobile number if it exists.
     *
     * @param mobileNumber the recipient's phone number
     * @return a map with status and optionally the mock code
     */
    @Transactional
    public Map<String, Object> sendOtp(String mobileNumber) {
        log.info("Request to send OTP for mobile: {}", mobileNumber);

        // 1. Sanitize & validate format
        if (mobileNumber == null || mobileNumber.isBlank()) {
            throw new CustomValidationException(HttpStatus.BAD_REQUEST.value(), "Mobile number is required!");
        }
        
        String cleanNumber = mobileNumber.replaceAll("[^0-9+]", "");
        if (cleanNumber.length() < 10) {
            throw new CustomValidationException(HttpStatus.BAD_REQUEST.value(), "Invalid mobile number format!");
        }

        // 2. Check existence in tblm_staff / tblm_customers
        boolean exists = false;
        
        // Strip plus sign for looking up in database in case it doesn't store '+'
        String searchNumber = cleanNumber.startsWith("+") ? cleanNumber.substring(1) : cleanNumber;
        
        Optional<Staff> staffOpt = staffRepository.findByMobileNumberAndIsDeleted(searchNumber, false);
        if (staffOpt.isEmpty() && cleanNumber.startsWith("+")) {
            // Try with original cleanNumber containing '+'
            staffOpt = staffRepository.findByMobileNumberAndIsDeleted(cleanNumber, false);
        }
        if (staffOpt.isEmpty() && searchNumber.length() > 10) {
            // Try with last 10 digits
            String last10 = searchNumber.substring(searchNumber.length() - 10);
            staffOpt = staffRepository.findByMobileNumberAndIsDeleted(last10, false);
        }

        Optional<Customer> customerOpt = customerRepository.findByMobileNoAndIsDelete(searchNumber, false);
        if (customerOpt.isEmpty() && cleanNumber.startsWith("+")) {
            // Try with original cleanNumber containing '+'
            customerOpt = customerRepository.findByMobileNoAndIsDelete(cleanNumber, false);
        }
        if (customerOpt.isEmpty() && searchNumber.length() > 10) {
            // Try with last 10 digits
            String last10 = searchNumber.substring(searchNumber.length() - 10);
            customerOpt = customerRepository.findByMobileNoAndIsDelete(last10, false);
        }

        if (staffOpt.isPresent() || customerOpt.isPresent()) {
            exists = true;
        }

        if (!exists) {
            throw new CustomValidationException(HttpStatus.BAD_REQUEST.value(), "Mobile number is not registered in the system!");
        }

        // 3. Generate 6-digit OTP code
        String otpCode = String.valueOf(100000 + random.nextInt(900000));

        // 4. Delete existing OTP entries for this number to clean up
        otpRepository.deleteByMobileNumber(cleanNumber);
        otpRepository.deleteByMobileNumber(searchNumber);

        // 5. Save new OTP verification entry
        OtpVerification verification = new OtpVerification();
        verification.setMobileNumber(cleanNumber);
        verification.setOtpCode(otpCode);
        verification.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        verification.setVerified(false);
        otpRepository.save(verification);

        // 6. Send OTP via the generic notification framework (SMS channel)
        Map<String, Object> variables = Map.of(
                "otpCode", otpCode
        );
        
        eventPublisher.publishEvent(new GenericNotificationEvent(
                "OTP_SMS",
                List.of(cleanNumber),
                NotificationChannel.SMS,
                variables
        ));

        // 7. Return response map
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "OTP sent successfully.");
        if (mockOtpEnabled) {
            response.put("mockOtp", otpCode);
        }
        return response;
    }

    /**
     * Verifies the OTP code for the mobile number and logs the user in (issuing JWT).
     *
     * @param mobileNumber the recipient's phone number
     * @param otpCode      the OTP code submitted
     * @return TokenResponse containing the JWT token
     */
    @Transactional
    public TokenResponse verifyOtpAndLogin(String mobileNumber, String otpCode) {
        log.info("Request to verify OTP for mobile: {}", mobileNumber);

        if (mobileNumber == null || mobileNumber.isBlank() || otpCode == null || otpCode.isBlank()) {
            throw new CustomValidationException(HttpStatus.BAD_REQUEST.value(), "Mobile number and OTP code are required!");
        }

        String cleanNumber = mobileNumber.replaceAll("[^0-9+]", "");
        String searchNumber = cleanNumber.startsWith("+") ? cleanNumber.substring(1) : cleanNumber;

        // 1. Fetch OTP record
        Optional<OtpVerification> verificationOpt = otpRepository.findTopByMobileNumberOrderByCreatedAtDesc(cleanNumber);
        if (verificationOpt.isEmpty()) {
            verificationOpt = otpRepository.findTopByMobileNumberOrderByCreatedAtDesc(searchNumber);
        }

        if (verificationOpt.isEmpty()) {
            throw new CustomValidationException(HttpStatus.BAD_REQUEST.value(), "OTP not sent or invalid!");
        }

        OtpVerification verification = verificationOpt.get();

        // 2. Validate OTP expiry & matching
        if (verification.isVerified()) {
            throw new CustomValidationException(HttpStatus.BAD_REQUEST.value(), "OTP already verified and used!");
        }

        if (verification.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new CustomValidationException(HttpStatus.BAD_REQUEST.value(), "OTP has expired! Please request a new one.");
        }

        if (!verification.getOtpCode().equals(otpCode)) {
            throw new CustomValidationException(HttpStatus.BAD_REQUEST.value(), "Incorrect OTP code. Please try again!");
        }

        // 3. Mark as verified and clean up
        verification.setVerified(true);
        otpRepository.delete(verification);

        // 4. Look up user by mobile number to issue token
        Optional<Staff> staffOpt = staffRepository.findByMobileNumberAndIsDeleted(searchNumber, false);
        if (staffOpt.isEmpty() && cleanNumber.startsWith("+")) {
            staffOpt = staffRepository.findByMobileNumberAndIsDeleted(cleanNumber, false);
        }
        if (staffOpt.isEmpty() && searchNumber.length() > 10) {
            String last10 = searchNumber.substring(searchNumber.length() - 10);
            staffOpt = staffRepository.findByMobileNumberAndIsDeleted(last10, false);
        }

        if (staffOpt.isPresent()) {
            Staff staff = staffOpt.get();
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

        Optional<Customer> customerOpt = customerRepository.findByMobileNoAndIsDelete(searchNumber, false);
        if (customerOpt.isEmpty() && cleanNumber.startsWith("+")) {
            customerOpt = customerRepository.findByMobileNoAndIsDelete(cleanNumber, false);
        }
        if (customerOpt.isEmpty() && searchNumber.length() > 10) {
            String last10 = searchNumber.substring(searchNumber.length() - 10);
            customerOpt = customerRepository.findByMobileNoAndIsDelete(last10, false);
        }

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            LoggedInUser loggedInUser = new LoggedInUser();
            loggedInUser.setStaffId(customer.getCustomerId()); // maps to JWT subject
            loggedInUser.setUuid(customer.getUuid());
            loggedInUser.setUserName(customer.getEmail() != null ? customer.getEmail() : customer.getMobileNo());
            loggedInUser.setFirstName(customer.getFirstName());
            loggedInUser.setLastName(customer.getLastName());
            loggedInUser.setEmail(customer.getEmail());
            loggedInUser.setCompanyId(customer.getCompanyId());
            loggedInUser.setRoles(Set.of("CUSTOMER"));
            loggedInUser.setPermissions(new HashSet<>());

            String token = jwtUtils.generateToken(loggedInUser);
            return new TokenResponse(token, jwtUtils.getTokenExpirationTime());
        }

        throw new CustomValidationException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to log in: user record disappeared!");
    }
}
