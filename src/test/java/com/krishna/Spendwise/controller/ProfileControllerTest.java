package com.krishna.Spendwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishna.Spendwise.domain.dto.AuthDto;
import com.krishna.Spendwise.domain.dto.ProfileDto;
import com.krishna.Spendwise.security.JwtRequestFilter;
import com.krishna.Spendwise.service.ProfileService;
import com.krishna.Spendwise.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

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
    void shouldRegisterProfile() throws Exception {

        ProfileDto dto = ProfileDto.builder()
                .email("test@mail.com")
                .build();

        when(profileService.registerProfile(any())).thenReturn(dto);

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldActivateProfile() throws Exception {

        when(profileService.ActivateProfile("token123")).thenReturn(true);

        mockMvc.perform(get("/activate")
                        .param("token", "token123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Activated"));
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {

        AuthDto auth = AuthDto.builder()
                .email("test@mail.com")
                .password("1234")
                .build();

        when(profileService.isAccountActive("test@mail.com")).thenReturn(true);
        when(profileService.authenticateAndGenerateToken(any()))
                .thenReturn(Map.of("token", "jwt-token"));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void shouldReturnForbiddenIfAccountInactive() throws Exception {

        AuthDto auth = AuthDto.builder()
                .email("test@mail.com")
                .password("1234")
                .build();

        when(profileService.isAccountActive("test@mail.com")).thenReturn(false);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(auth)))
                .andExpect(status().isForbidden());
    }


}
