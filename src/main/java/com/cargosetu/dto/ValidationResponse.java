package com.cargosetu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ValidationResponse {
    private boolean isValid = false;
    private String message = "FAIL";

    public ValidationResponse(boolean isValid, String message) {
        this.isValid = isValid;
        this.message = message;
    }
}
