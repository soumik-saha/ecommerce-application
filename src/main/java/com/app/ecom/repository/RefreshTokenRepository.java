package com.app.ecom.repository;

import com.app.ecom.model.RefreshToken;
import com.app.ecom.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE com_refresh_tokens rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    int revokeAllByUser(@Param("user") User user);
}
