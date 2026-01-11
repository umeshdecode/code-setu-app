package com.cargosetu.config.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto {
    private Integer responseCode;
    private String responseMessage;
    private Object responseBody;
}
