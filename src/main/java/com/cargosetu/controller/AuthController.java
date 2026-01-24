package com.cargosetu.controller;

import com.cargosetu.config.dto.ApiResponseDto;
import com.cargosetu.config.dto.LoginRequest;
import com.cargosetu.config.dto.TokenResponse;
import com.cargosetu.config.JwtUtils;
import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.dto.StaffDTO;
import com.cargosetu.repository.StaffRepository;
import com.cargosetu.service.AuthService;
import com.cargosetu.service.StaffService;
import com.cargosetu.utils.CommonConstants;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


    @PostMapping(UrlConstants.LOGIN_URL)
    public ResponseEntity<?> login(@RequestBody LoginRequest loginDto, HttpServletRequest request) {
        TokenResponse tokenResponse = new TokenResponse();
        boolean validRequest = authService.isValidRequest(loginDto);
        if (validRequest) {
            tokenResponse = authService.loginUser(loginDto);
            if (tokenResponse.getAccessToken() == null) {
                ApiResponseDto responseDto = generateResponse(HttpStatus.EXPECTATION_FAILED.value(), "Something went wrong!!", tokenResponse);
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(responseDto);
            }
            return ResponseEntity.status(HttpStatus.OK).body(tokenResponse);
        }
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Something went wrong");
    }

    @PostMapping(UrlConstants.REGISTER_USER)
    public GenericDataDTO registerUser(@RequestBody StaffDTO staffDTO) {
        GenericDataDTO dto = new GenericDataDTO();
        boolean validated = staffService.validateCreateStaffRequest(staffDTO);
        Boolean duplicateUser = staffService.isDuplicateUser(staffDTO);
        if (validated) {
            if (duplicateUser) {
                dto.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
                dto.setResponseMessege("Username already exists");
                return dto;
            }
            StaffDTO staff = staffService.saveStaff(staffDTO);
            if (Objects.nonNull(staff)) {
                dto.setData(staff);
                dto.setResponseCode(CommonStatusCode.CREATED);
                dto.setResponseMessege(CommonConstants.STAFF + CommonStatusCode.MSG_CREATED);
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
}
