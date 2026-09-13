package com.cargosetu.handler;

import com.cargosetu.config.dto.ApiResponseDto;
import com.cargosetu.utils.CommonStatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomValidationException.class)
    public ResponseEntity<ApiResponseDto> handleCustomValidation(
            CustomValidationException ex) {

        log.warn("Validation error: {}", ex.getMessage());

        ApiResponseDto responseDto =  new ApiResponseDto(ex.getErrorCode(), ex.getMessage(), null);

        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseDto);
    }

    /**
     * Fires for every {@code @Valid @RequestBody}/{@code @ModelAttribute} DTO that fails
     * Bean Validation (e.g. a negative charge, a malformed pincode). Without this handler
     * these fall through to the generic Exception handler below and lose their field-level
     * messages, so callers only ever see "Something went wrong".
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto> handleValidation(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = CommonStatusCode.MSG_VALIDATION_FAILED;
        }

        log.warn("Request validation failed: {}", message);

        ApiResponseDto responseDto = new ApiResponseDto(CommonStatusCode.VALIDATION_ERROR, message, null);

        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseDto);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto> handleExceptionValidation(
            Exception ex) {

        ex.printStackTrace();
        log.warn("Exception occurred, error: {}", ex.getMessage());

        ApiResponseDto responseDto =  new ApiResponseDto(CommonStatusCode.INTERNAL_SERVER_ERROR, ex.getMessage(), null);
        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseDto);
    }
}
