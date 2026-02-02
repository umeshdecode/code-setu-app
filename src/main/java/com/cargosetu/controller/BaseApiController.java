package com.cargosetu.controller;


import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.dto.TokenResponse;
import com.cargosetu.service.AuthService;
import com.cargosetu.utils.APIConstants;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

@Slf4j
@RequestMapping(BaseURL.BASE_URL)
@RestController
public class BaseApiController {

    @Autowired
    private AuthService authService;



    @GetMapping(value = "/refreshtoken")
    public ResponseEntity<?> refreshtoken(HttpServletRequest request) {
        HashMap<String, Object> response = new HashMap<>();
        try {
            String bearerToken = request.getHeader("Authorization");
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                String oldToken = bearerToken.substring(7);

                // 2. Delegate to service to generate a new token
                TokenResponse newToken = authService.refreshToken(oldToken);

                return ResponseEntity.ok(newToken);
            }
            log.info("Refresh token successful; Response : {message:{}, code:{}}", HttpStatus.OK, APIConstants.SUCCESS);
        } catch (Exception ex) {
            ex.printStackTrace();
            response.put(CommonConstants.ERROR_TAG, HttpStatus.EXPECTATION_FAILED.getReasonPhrase());
            log.error("Unable to refresh token request;  Exception:{}", ex.getStackTrace());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Refresh Request");
    }


    public ResponseEntity<?> apiResponse(Integer responseCode, HashMap<String, Object> response) {
        try {
            response.put("timestamp", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSSS").format(LocalDateTime.now()));
            response.put("status", responseCode);

            log.info("Final Response:" + new ObjectMapper().writeValueAsString(response));
            if (responseCode.equals(APIConstants.SUCCESS)) {
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else if (responseCode.equals(APIConstants.FAIL)) {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            } else if (responseCode.equals(APIConstants.INTERNAL_SERVER_ERROR)) {
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }else if (responseCode.equals(APIConstants.NO_CONTENT_FOUND)) {
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else {
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (response == null) {
                response = new HashMap<>();
            }
            response.put("status", APIConstants.INTERNAL_SERVER_ERROR);
            response.put(APIConstants.ERROR_TAG, e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
