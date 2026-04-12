package com.app.ecom.service;

import com.app.ecom.dto.AuthRequest;
import com.app.ecom.dto.AuthResponse;
import com.app.ecom.dto.RefreshTokenRequest;
import com.app.ecom.model.Address;
import com.app.ecom.model.RefreshToken;
import com.app.ecom.model.User;
import com.app.ecom.model.UserRole;
import com.app.ecom.repository.UserRepository;
import com.app.ecom.dto.RegisterRequest;
import com.app.ecom.security.AppUserDetails;
import com.app.ecom.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.auth.admin-registration-secret}")
    private String adminRegistrationSecret;

    @Value("${app.jwt.expiration-ms}")
    private long accessTokenExpirationMs;

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering customer account for email={}", request.getEmail());
        return registerWithRole(request, UserRole.CUSTOMER);
    }

    public AuthResponse registerAdmin(RegisterRequest request, String providedSecret) {
        if (providedSecret == null || !providedSecret.equals(adminRegistrationSecret)) {
            log.warn("Admin registration rejected for email={} due to invalid secret", request.getEmail());
            throw new IllegalArgumentException("Invalid admin registration secret");
        }

        log.info("Registering admin account for email={}", request.getEmail());
        return registerWithRole(request, UserRole.ADMIN);
    }

    @Transactional
    public void logout(User user) {
        refreshTokenService.revokeAllForUser(user);
        log.info("Logout completed for userId={}", user.getId());
    }

    private AuthResponse registerWithRole(RegisterRequest request, UserRole role) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected because email already exists: {}", request.getEmail());
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        Address address = new Address();
        address.setStreet(request.getAddress().getStreet());
        address.setCity(request.getAddress().getCity());
        address.setState(request.getAddress().getState());
        address.setZipcode(request.getAddress().getZipcode());
        address.setCountry(request.getAddress().getCountry());
        user.setAddress(address);

        User saved = userRepository.save(user);
        log.info("User account created with id={}, role={}", saved.getId(), saved.getRole());
        AppUserDetails userDetails = new AppUserDetails(saved);
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(saved);

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                saved.getId(),
                saved.getEmail(),
                saved.getRole().name(),
                Instant.now().plusMillis(accessTokenExpirationMs),
                refreshToken.getExpiryDate()
        );
    }

    public AuthResponse login(AuthRequest request) {
        try {
            log.info("Authenticating user with email={}", request.getEmail());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
            String accessToken = jwtService.generateToken(userDetails);

            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            return new AuthResponse(
                    accessToken,
                    refreshToken.getToken(),
                    "Bearer",
                    userDetails.getId(),
                    userDetails.getEmail(),
                    userDetails.getRole(),
                    Instant.now().plusMillis(accessTokenExpirationMs),
                    refreshToken.getExpiryDate()
            );
        } catch (BadCredentialsException ex) {
            log.warn("Login failed for email={}", request.getEmail());
            throw new IllegalArgumentException("Invalid email or password");
        }
    }

    @Transactional
    public AuthResponse refreshAccessToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken());
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        AppUserDetails userDetails = new AppUserDetails(user);
        String newAccessToken = jwtService.generateToken(userDetails);

        log.info("Access token refreshed for userId={}", user.getId());
        return new AuthResponse(
                newAccessToken,
                refreshToken.getToken(),
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                Instant.now().plusMillis(accessTokenExpirationMs),
                refreshToken.getExpiryDate()
        );
    }
}

