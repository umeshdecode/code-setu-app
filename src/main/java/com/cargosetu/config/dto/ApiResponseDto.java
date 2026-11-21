package com.cargosetu.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto {
    private Integer responseCode;

    private String responseMessage;

    private Object responseBody;

    private List<?> dataList;

    private LocalDateTime timestamp;

    public ApiResponseDto(Integer responseCode, String responseMessage, Object responseBody) {
        this.responseBody=responseBody;
        this.responseCode=responseCode;
        this.responseMessage=responseMessage;
    }
}
