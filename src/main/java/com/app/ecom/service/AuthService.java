package com.app.ecom.service;

import com.app.ecom.dto.AuthRequest;
import com.app.ecom.dto.AuthResponse;
import com.app.ecom.dto.RegisterRequest;
import com.app.ecom.model.Address;
import com.app.ecom.model.User;
import com.app.ecom.model.UserRole;
import com.app.ecom.repository.UserRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${app.auth.admin-registration-secret}")
    private String adminRegistrationSecret;

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

    public void logout() {
        // JWT auth is stateless; clients should discard the token after logout.
        log.info("Logout acknowledged for stateless JWT session");
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
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, "Bearer", saved.getId(), saved.getEmail(), saved.getRole().name());
    }

    public AuthResponse login(AuthRequest request) {
        try {
            log.info("Authenticating user with email={}", request.getEmail());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
            String token = jwtService.generateToken(userDetails);

            return new AuthResponse(
                    token,
                    "Bearer",
                    userDetails.getId(),
                    userDetails.getEmail(),
                    userDetails.getRole()
            );
        } catch (BadCredentialsException ex) {
            log.warn("Login failed for email={}", request.getEmail());
            throw new IllegalArgumentException("Invalid email or password");
        }
    }
}

