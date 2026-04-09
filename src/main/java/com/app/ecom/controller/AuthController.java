package com.app.ecom.controller;

import com.app.ecom.dto.AuthRequest;
import com.app.ecom.dto.AuthResponse;
import com.app.ecom.dto.RefreshTokenRequest;
import com.app.ecom.dto.RegisterRequest;
import com.app.ecom.model.User;
import com.app.ecom.repository.UserRepository;
import com.app.ecom.security.AppUserDetails;
import com.app.ecom.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Auth register request received for email={} ", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/register/admin")
    public ResponseEntity<AuthResponse> registerAdmin(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader("X-Admin-Secret") String adminSecret
    ) {
        log.info("Admin register request received for email={}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerAdmin(request, adminSecret));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("Login request received for email={}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");
        return ResponseEntity.ok(authService.refreshAccessToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal AppUserDetails currentUser) {
        log.info("Logout request received for userId={}", currentUser.getId());
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        authService.logout(user);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}

