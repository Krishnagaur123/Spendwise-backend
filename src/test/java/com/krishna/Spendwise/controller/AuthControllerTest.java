package com.krishna.Spendwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishna.Spendwise.controller.api.AuthController;
import com.krishna.Spendwise.domain.dto.ProfileDto;
import com.krishna.Spendwise.domain.dto.api.LoginRequest;
import com.krishna.Spendwise.domain.dto.api.RegisterRequest;
import com.krishna.Spendwise.security.JwtRequestFilter;
import com.krishna.Spendwise.service.ProfileService;
import com.krishna.Spendwise.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_shouldReturnCreated() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("test@test.com");
        request.setPassword("password");
        request.setFullName("Test User");
        request.setPhoneNumber("1234567890");

        when(profileService.registerWithoutActivation(any())).thenReturn(1L);

        // WebMvcTest does NOT apply server.servlet.context-path, so path is /auth/* not /api/auth/*
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    void login_shouldReturnOk() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("test@test.com");
        request.setPassword("password");

        ProfileDto mockProfile = ProfileDto.builder().email("test@test.com").build();
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getName()).thenReturn("test@test.com");
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
        when(jwtUtil.generateToken("test@test.com")).thenReturn("mocked-jwt-token");
        // Map.of() rejects null values — stub getPublicProfile so 'user' is non-null
        when(profileService.getPublicProfile("test@test.com")).thenReturn(mockProfile);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token"));
    }

    @Test
    void logout_shouldReturnOk() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }
}
