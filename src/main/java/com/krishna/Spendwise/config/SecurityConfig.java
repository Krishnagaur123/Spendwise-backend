package com.krishna.Spendwise.config;

import com.krishna.Spendwise.security.JwtRequestFilter;
import com.krishna.Spendwise.service.AppUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central Spring Security configuration.
 *
 * <p>The app intentionally supports two authentication strategies in parallel:
 * <ul>
 *   <li><b>JWT</b> — React frontend stores the token in localStorage and sends it as
 *       {@code Authorization: Bearer <token>}. Handled by {@link com.krishna.Spendwise.security.JwtRequestFilter}.</li>
 *   <li><b>Session</b> — Assignment API clients use the {@code JSESSIONID} cookie issued on
 *       login. {@link HttpSessionSecurityContextRepository} is wired explicitly so session auth
 *       persists correctly across requests.</li>
 * </ul>
 * CSRF is disabled because both auth paths are CSRF-resistant by design.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final AppUserDetailsService appUserDetailsService;
    private final JwtRequestFilter jwtRequestFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        // Explicit session repository wiring — without this, session auth is lost between requests.
        HttpSessionSecurityContextRepository sessionRepo = new HttpSessionSecurityContextRepository();

        httpSecurity.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .securityContext(ctx -> ctx.securityContextRepository(sessionRepo))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/status", "/health", "/register", "/activate", "/login",
                                "/auth/register", "/auth/login", "/auth/logout").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                // Return 401 JSON for unauthenticated requests — prevents Spring from issuing a 302 redirect
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Authentication required\"}");
                        })
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS config for cross-origin requests from the React dev server (port 3000 → 8080).
     * {@code allowedOriginPatterns("*")} is required when {@code allowCredentials(true)} is set —
     * a literal wildcard origin is rejected by the Fetch spec in that combination.
     * Restrict to your actual frontend domain in production.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Cookie"));
        // Required so the browser can read and store Set-Cookie in cross-origin responses
        configuration.setExposedHeaders(List.of("Set-Cookie"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /** {@link AuthenticationManager} backed by our UserDetailsService and BCrypt encoder. */
    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(appUserDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(authenticationProvider);
    }

}
