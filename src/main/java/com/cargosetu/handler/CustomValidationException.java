package com.cargosetu.handler;

import lombok.Getter;

@Getter
public class CustomValidationException extends RuntimeException{

    private final Integer errorCode;

    public CustomValidationException(Integer errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
    }

    public CustomValidationException(Integer errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
    }}
