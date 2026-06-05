package com.cargosetu.handler;

import com.cargosetu.config.dto.ApiResponseDto;
import com.cargosetu.utils.CommonStatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

//    @ExceptionHandler(CustomValidationException.class)
//    public ResponseEntity<ApiResponseDto> handleCustomValidation(
//            CustomValidationException ex) {
//
//        log.warn("Validation error: {}", ex.getMessage());
//
//        ApiResponseDto responseDto =  new ApiResponseDto(ex.getErrorCode(), ex.getMessage(), null);
//
//        return ResponseEntity
//                .badRequest()
//                .body(responseDto);
//    }
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiResponseDto> handleExceptionValidation(
//            Exception ex) {
//
//        log.warn("Exception occurred, error: {}", ex.getMessage());
//
//        ApiResponseDto responseDto =  new ApiResponseDto(CommonStatusCode.INTERNAL_SERVER_ERROR, ex.getMessage(), null);
//        return ResponseEntity
//                .badRequest()
//                .body(responseDto);
//    }
}
