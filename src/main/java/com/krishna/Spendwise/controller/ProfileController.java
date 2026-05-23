package com.krishna.Spendwise.controller;

import com.krishna.Spendwise.domain.dto.AuthDto;
import com.krishna.Spendwise.domain.dto.ProfileDto;
import com.krishna.Spendwise.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<ProfileDto> registerProfile(@RequestBody ProfileDto profileDto) {
        ProfileDto registeredProfile = profileService.registerProfile(profileDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredProfile);
    }

    @GetMapping("/activate")
    public ResponseEntity<String> activateProfile(@RequestParam String token) {
        boolean isActivated = profileService.ActivateProfile(token);
        if (isActivated) {
            return ResponseEntity.status(HttpStatus.OK).body("Activated");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invalid activation token");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthDto authDTO, HttpServletRequest httpRequest) {
        try {
            if (!profileService.isAccountActive(authDTO.getEmail())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "message", "Account is not active. Please activate your account first."));
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authDTO.getEmail(), authDTO.getPassword()));
            SecurityContext sc = SecurityContextHolder.getContext();
            sc.setAuthentication(authentication);
            HttpSession session = httpRequest.getSession(true);
            // Use the constant from HttpSessionSecurityContextRepository to ensure
            // the correct session attribute key is used for security context persistence.
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

            Map<String, Object> response = profileService.authenticateAndGenerateToken(authDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage()));
        }
    }

    /**
     * GET /api/v1.0/profile
     *
     * Returns the current user's profile. Works for BOTH authentication modes:
     *  - JWT: The JwtRequestFilter sets Authentication in SecurityContext from the Bearer token.
     *  - Session: Spring Security restores Authentication from the HTTP session automatically.
     *
     * By injecting Authentication directly (or @AuthenticationPrincipal), we avoid
     * any manual token parsing that previously caused 400 errors.
     *
     * Unauthenticated requests will be rejected by Spring Security BEFORE reaching this
     * method and will receive 401 (not 400).
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required"));
        }
        ProfileDto profile = profileService.getPublicProfile(authentication.getName());
        return ResponseEntity.ok(Map.of("user", profile));
    }

    @GetMapping("/test")
    public String test() {
        return "Test successfull";
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }
}
