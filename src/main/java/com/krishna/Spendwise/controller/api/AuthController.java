package com.krishna.Spendwise.controller.api;

import com.krishna.Spendwise.domain.dto.api.LoginRequest;
import com.krishna.Spendwise.domain.dto.api.MessageResponse;
import com.krishna.Spendwise.domain.dto.api.RegisterRequest;
import com.krishna.Spendwise.service.ProfileService;
import com.krishna.Spendwise.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Primary authentication endpoint for the assignment API ({@code /auth/*}).
 *
 * <p>{@code POST /auth/login} establishes both a server-side session and returns a JWT,
 * so the React frontend (localStorage/Bearer) and assignment test clients (session cookie)
 * can both authenticate via a single login call without modification.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final ProfileService profileService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /** Registers a new user. Account is immediately active — no email verification required. */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = profileService.registerWithoutActivation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "User registered successfully",
                "userId", userId
        ));
    }

    /**
     * Authenticates the user and returns {@code { token, user }}.
     * Also creates a server-side session (JSESSIONID) so subsequent requests can use
     * either the Bearer token or the session cookie interchangeably.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                     HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // Persist auth in both SecurityContext and HTTP session for dual-auth support
        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(authentication);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

        String token = jwtUtil.generateToken(authentication.getName());
        Object user = profileService.getPublicProfile(authentication.getName());

        return ResponseEntity.ok(Map.of("token", token, "user", user));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }
}
