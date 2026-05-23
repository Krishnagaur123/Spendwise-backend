package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.dto.AuthDto;
import com.krishna.Spendwise.domain.dto.ProfileDto;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
import com.krishna.Spendwise.repository.ProfileRepository;
import com.krishna.Spendwise.repository.CategoryRepository;
import com.krishna.Spendwise.util.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {


    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProfileService profileService;

    private ProfileEntity profile;

    @BeforeEach
    void setup() {
        profile = ProfileEntity.builder()
                .id(1L)
                .email("test@mail.com")
                .password("encodedPass")
                .isActive(true)
                .build();
    }

    @Test
    void registerProfile_shouldSaveProfileAndSendEmail() {

        ProfileDto dto = ProfileDto.builder()
                .email("test@mail.com")
                .password("1234")
                .build();

        when(passwordEncoder.encode("1234")).thenReturn("encodedPass");
        when(profileRepository.save(any())).thenReturn(profile);

        ProfileDto result = profileService.registerProfile(dto);

        verify(profileRepository).save(any());
        verify(emailService).sendVerificationEmail(any(), any(), any());

        assertThat(result.getEmail()).isEqualTo("test@mail.com");
    }

    @Test
    void activateProfile_shouldReturnTrueIfTokenValid() {

        when(profileRepository.findByActivationToken("token123"))
                .thenReturn(Optional.of(profile));

        boolean result = profileService.ActivateProfile("token123");

        assertThat(result).isTrue();
        verify(profileRepository).save(profile);
    }

    @Test
    void activateProfile_shouldReturnFalseIfTokenInvalid() {

        when(profileRepository.findByActivationToken("badToken"))
                .thenReturn(Optional.empty());

        boolean result = profileService.ActivateProfile("badToken");

        assertThat(result).isFalse();
    }

    @Test
    void isAccountActive_shouldReturnTrueIfActive() {

        when(profileRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.of(profile));

        boolean result = profileService.isAccountActive("test@mail.com");

        assertThat(result).isTrue();
    }

    @Test
    void authenticateAndGenerateToken_shouldReturnToken() {

        AuthDto auth = AuthDto.builder()
                .email("test@mail.com")
                .password("1234")
                .build();

        when(jwtUtil.generateToken("test@mail.com")).thenReturn("jwt-token");
        when(profileRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(profile));

        Map<String, Object> result = profileService.authenticateAndGenerateToken(auth);

        assertThat(result.get("token")).isEqualTo("jwt-token");
    }


}
