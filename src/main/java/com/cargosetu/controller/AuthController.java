package com.cargosetu.controller;

import com.cargosetu.config.dto.ApiResponseDto;
import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.dto.LoginRequest;
import com.cargosetu.config.dto.TokenResponse;
import com.cargosetu.config.JwtUtils;
import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.dto.StaffDTO;
import com.cargosetu.handler.CustomValidationException;
import com.cargosetu.push.service.PushTokenService;
import com.cargosetu.repo.StaffRepository;
import com.cargosetu.service.AuthService;
import com.cargosetu.service.StaffService;
import com.cargosetu.utils.CommonConstants;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping(UrlConstants.AUTH)
@Slf4j
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StaffRepository staffRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private StaffService staffService;
    @Autowired
    private PushTokenService pushTokenService;


    @PostMapping(UrlConstants.LOGIN_URL)
    public ResponseEntity<?> login(@RequestBody LoginRequest loginDto, HttpServletRequest request) {
        TokenResponse tokenResponse = new TokenResponse();
        try {
            boolean validRequest = authService.isValidRequest(loginDto);
            if (validRequest) {
                tokenResponse = authService.loginUser(loginDto);
                if (tokenResponse.getAccessToken() == null) {
                    ApiResponseDto responseDto = generateResponse(HttpStatus.EXPECTATION_FAILED.value(), "Something went wrong!!", tokenResponse);
                    return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(responseDto);
                }
                return ResponseEntity.status(HttpStatus.OK).body(tokenResponse);
            }
        } catch (CustomValidationException e) {
            return ResponseEntity.status(HttpStatus.OK).body(e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Something went wrong");
    }

    @PostMapping("/check-user")
    public ResponseEntity<Map<String, Boolean>> checkUser(@RequestBody CheckUserRequest request) {
        boolean exists = authService.userExists(request.getEmail());
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PostMapping(UrlConstants.REGISTER_USER)
    public GenericDataDTO registerUser(@RequestBody StaffDTO staffDTO) {
        GenericDataDTO dto = new GenericDataDTO();
        boolean validated = staffService.validateCreateStaffRequest(staffDTO);
        Boolean duplicateUser = staffService.isDuplicateUser(staffDTO);
        if (validated) {
            if (duplicateUser) {
                dto.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
                dto.setResponseMessege("Username/email already exists");
                return dto;
            }
            StaffDTO staff = staffService.saveStaff(staffDTO,true);
            if (Objects.nonNull(staff)) {
                dto.setData(staff);
                dto.setResponseCode(CommonStatusCode.CREATED);
                dto.setResponseMessege("You are registered successfully!");
            } else {
                dto.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
                dto.setResponseMessege(CommonStatusCode.MSG_INTERNAL_SERVER_ERROR);
            }
        }
        return dto;
    }

    public ApiResponseDto generateResponse(Integer responseCode, String responseMessage, Object responseBody) {
        return new ApiResponseDto(responseCode, responseMessage, responseBody);
    }

    /**
     * General session-end hook, mapped under UrlConstants.LOGOUT_URL which was
     * previously declared but never mapped to a controller method. There is no
     * server-side session/token blacklist yet, so this is primarily the
     * push-unregister hook for the currently logged-out device: if a deviceId
     * is supplied, that installation's push token is deactivated so it stops
     * receiving notifications for an account that is no longer signed in on it.
     */
    @PostMapping(UrlConstants.LOGOUT_URL)
    public ResponseEntity<?> logout(@AuthenticationPrincipal LoggedInUser user,
                                     @RequestBody(required = false) LogoutRequest request) {
        log.info("API: Logout for user={}", user != null ? user.getUsername() : null);
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (user != null && request != null && request.getDeviceId() != null && !request.getDeviceId().isBlank()) {
                pushTokenService.deactivateCurrent(user, request.getDeviceId());
            }
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Logged out successfully.");
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @Data
    public static class CheckUserRequest {
        private String email;
    }

    @Data
    public static class LogoutRequest {
        private String deviceId;
    }
}
