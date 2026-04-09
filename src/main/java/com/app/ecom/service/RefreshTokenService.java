package com.app.ecom.service;

import com.app.ecom.exception.TokenRefreshException;
import com.app.ecom.model.RefreshToken;
import com.app.ecom.model.User;
import com.app.ecom.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke existing tokens for the user before issuing a new one
        refreshTokenRepository.revokeAllByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));
        refreshToken.setRevoked(false);

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.debug("Issued refresh token for userId={}", user.getId());
        return saved;
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            log.warn("Revoked refresh token used for userId={}", token.getUser().getId());
            throw new TokenRefreshException("Refresh token has been revoked. Please log in again.");
        }
        if (token.getExpiryDate().isBefore(Instant.now())) {
            log.warn("Expired refresh token used for userId={}", token.getUser().getId());
            throw new TokenRefreshException("Refresh token has expired. Please log in again.");
        }
        return token;
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenRefreshException("Refresh token not found or already used."));
    }

    @Transactional
    public void revokeAllForUser(User user) {
        int count = refreshTokenRepository.revokeAllByUser(user);
        log.debug("Revoked {} refresh tokens for userId={}", count, user.getId());
    }
}
