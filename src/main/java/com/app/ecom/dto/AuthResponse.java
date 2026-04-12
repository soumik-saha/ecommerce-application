package com.app.ecom.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long userId;
    private String email;
    private String role;
    private Instant accessTokenExpiresAt;
    private Instant refreshTokenExpiresAt;

    public AuthResponse(String accessToken,
                        String refreshToken,
                        String tokenType,
                        Long userId,
                        String email,
                        String role,
                        Instant accessTokenExpiresAt,
                        Instant refreshTokenExpiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }
}

