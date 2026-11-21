package com.cargosetu.dto;

import lombok.Data;

@Data
public class InquiryRequest {

    private String name;
    private String email;
    private String phone;
    private String subject;
    private String message;
}