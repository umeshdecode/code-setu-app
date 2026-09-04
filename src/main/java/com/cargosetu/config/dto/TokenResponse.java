package com.cargosetu.config.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String tokenType = "Bearer ";
    private Long expiresIn;

    private Long staffId;
    private int companyId;
    private String userName;
    private String email;
    private Set<String> roles;
    private Set<String> permissions;
    private Map<String, Set<String>> moduleRights;

    public TokenResponse(String accessToken, Long expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }
}
