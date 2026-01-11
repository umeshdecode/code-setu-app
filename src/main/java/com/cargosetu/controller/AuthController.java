package com.cargosetu.controller;

import com.cargosetu.config.dto.ApiResponseDto;
import com.cargosetu.config.dto.LoginRequest;
import com.cargosetu.config.dto.TokenResponse;
import com.cargosetu.config.JwtUtils;
import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.repository.StaffRepository;
import com.cargosetu.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


    @PostMapping(UrlConstants.LOGIN_URL)
    public ResponseEntity<?> login(@RequestBody LoginRequest loginDto, HttpServletRequest request){
        try {
            TokenResponse tokenResponse = new TokenResponse();
            boolean validRequest = authService.isValidRequest(loginDto);
            if(validRequest){
                tokenResponse = authService.loginUser(loginDto);
                if(tokenResponse.getAccessToken() == null){
                    ApiResponseDto responseDto = generateResponse(HttpStatus.EXPECTATION_FAILED.value(), "Something went wrong!!", tokenResponse);
                    return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(responseDto);
                }
                return  ResponseEntity.status(HttpStatus.OK).body(tokenResponse);
            }
        } catch (Exception e) {
            log.error("error while login; Message: {}",e.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(HttpStatus.UNAUTHORIZED);
    }

    public ApiResponseDto generateResponse(Integer responseCode,String responseMessage,Object responseBody){
        return new ApiResponseDto(responseCode,responseMessage,responseBody);
    }
}
