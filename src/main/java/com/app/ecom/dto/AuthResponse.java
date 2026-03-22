package com.app.ecom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private Long userId;
    private String email;
    private String role;
}

