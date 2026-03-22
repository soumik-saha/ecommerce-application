package com.app.ecom.controller;

import com.app.ecom.dto.AuthRequest;
import com.app.ecom.dto.AuthResponse;
import com.app.ecom.dto.RegisterRequest;
import com.app.ecom.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

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

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        log.info("Logout request received");
        authService.logout();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}

