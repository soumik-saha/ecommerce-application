package com.app.ecom.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (!isPublicEndpoint(request)) {
                log.warn("Missing/invalid Authorization header for protected endpoint method='{}' path='{}'",
                        request.getMethod(), request.getRequestURI());
            }
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        String username;

        try {
            username = jwtService.extractUsername(jwt);
        } catch (Exception ex) {
            log.warn("JWT parsing failed for path='{}': {}", request.getRequestURI(), ex.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated request for user='{}' path='{}'", username, request.getRequestURI());
            } else {
                log.warn("Invalid JWT token for user='{}' path='{}'", username, request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (pathMatcher.match("/api/auth/register", uri)
                || pathMatcher.match("/api/auth/register/admin", uri)
                || pathMatcher.match("/api/auth/login", uri)
                || pathMatcher.match("/h2-console/**", uri)) {
            return true;
        }

        return "GET".equalsIgnoreCase(method) && pathMatcher.match("/api/products/**", uri);
    }
}

