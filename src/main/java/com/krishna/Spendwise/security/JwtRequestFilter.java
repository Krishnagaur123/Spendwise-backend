package com.krishna.Spendwise.security;

import com.krishna.Spendwise.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        String email = null;
        String jwt = null;

        // Only attempt JWT parsing if a Bearer token is present.
        // Any JWT exception (malformed, expired, bad signature) is caught here
        // so it does NOT propagate and cause a 400 from the GlobalExceptionHandler.
        // If JWT auth fails, we fall through and let Spring Security handle
        // the request (session auth will apply, or 401 will be returned).
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                email = jwtUtil.extractUsername(jwt);
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("JWT parsing failed for request [{}]: {}", request.getRequestURI(), e.getMessage());
                // Do not set authentication; let filter chain continue.
                // Unauthenticated protected endpoints will return 401 via Spring Security.
            }
        }

        // Only set authentication if:
        // 1. A valid email was extracted from JWT
        // 2. No authentication is already set (e.g., from session)
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("JWT authentication set for user: {}", email);
                }
            } catch (Exception e) {
                log.warn("JWT authentication failed for user [{}]: {}", email, e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
