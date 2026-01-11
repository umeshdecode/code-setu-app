package com.cargosetu.config.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String userName;
    private String password;
}
