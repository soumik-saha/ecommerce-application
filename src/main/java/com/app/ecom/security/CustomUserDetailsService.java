package com.app.ecom.security;

import com.app.ecom.model.User;
import com.app.ecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user details for email={}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            log.warn("User found without password for email={}", email);
            throw new UsernameNotFoundException("User has no password configured");
        }

        log.debug("User details loaded successfully for email={} with role={}", email, user.getRole());
        return new AppUserDetails(user);
    }
}

