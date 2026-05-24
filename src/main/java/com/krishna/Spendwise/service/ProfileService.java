package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.dto.AuthDto;
import com.krishna.Spendwise.domain.dto.ProfileDto;
import com.krishna.Spendwise.domain.entity.CategoryEntity;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
import com.krishna.Spendwise.exception.ConflictException;
import com.krishna.Spendwise.repository.CategoryRepository;
import com.krishna.Spendwise.repository.ProfileRepository;
import com.krishna.Spendwise.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles user registration, authentication, and profile access.
 *
 * <p>Two registration paths exist:
 * <ul>
 *   <li>{@link #registerProfile} — legacy flow with email activation token.</li>
 *   <li>{@link #registerWithoutActivation} — used by {@code /auth/register}; account is active immediately.</li>
 * </ul>
 *
 * <p>On registration, a default set of income/expense categories is seeded for the new user
 * so their first session has a pre-populated category list.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CategoryRepository categoryRepository;

    @Value("${app.activation.url}")
    private String activationUrl;

    /** Legacy registration with email verification. Email errors are swallowed so a missing mail config doesn't break registration. */
    public ProfileDto registerProfile(ProfileDto profileDto) {
        ProfileEntity newProfile = toEntity(profileDto);
        newProfile.setActivationToken(UUID.randomUUID().toString());
        ProfileEntity newEntity = profileRepository.save(newProfile);
        seedDefaultCategories(newEntity);

        String activationLink = activationUrl + "/api/v1.0/activate?token=" + newProfile.getActivationToken();
        try {
            emailService.sendVerificationEmail(newProfile.getEmail(), "Activate Profile",
                    "Click here to activate your profile: " + activationLink);
        } catch (Exception ignored) {
            // Mail not required for the app to function
        }
        return toDto(newEntity);
    }

    /**
     * Assignment-spec registration. Account is immediately active — no email step.
     *
     * @throws ConflictException if the email is already registered
     * @return the new user's database ID
     */
    public Long registerWithoutActivation(com.krishna.Spendwise.domain.dto.api.RegisterRequest request) {
        if (profileRepository.findByEmail(request.getUsername()).isPresent()) {
            throw new ConflictException("Email is already in use");
        }
        ProfileEntity newProfile = ProfileEntity.builder()
                .email(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .isActive(true)
                .build();
        newProfile = profileRepository.save(newProfile);
        seedDefaultCategories(newProfile);
        return newProfile.getId();
    }

    /**
     * Seeds 7 default categories (Salary + 6 common expense buckets) for a new user.
     * Categories are marked {@code isDefault=true} to prevent deletion by the user.
     */
    private void seedDefaultCategories(ProfileEntity profile) {
        List<CategoryEntity> defaults = List.of(
                CategoryEntity.builder().name("Salary").type("INCOME").isDefault(true).profile(profile).build(),
                CategoryEntity.builder().name("Food").type("EXPENSE").isDefault(true).profile(profile).build(),
                CategoryEntity.builder().name("Rent").type("EXPENSE").isDefault(true).profile(profile).build(),
                CategoryEntity.builder().name("Transportation").type("EXPENSE").isDefault(true).profile(profile).build(),
                CategoryEntity.builder().name("Entertainment").type("EXPENSE").isDefault(true).profile(profile).build(),
                CategoryEntity.builder().name("Healthcare").type("EXPENSE").isDefault(true).profile(profile).build(),
                CategoryEntity.builder().name("Utilities").type("EXPENSE").isDefault(true).profile(profile).build()
        );
        categoryRepository.saveAll(defaults);
    }

    public ProfileEntity toEntity(ProfileDto profileDto) {
        return ProfileEntity.builder()
                .id(profileDto.getId())
                .fullName(profileDto.getFullName())
                .email(profileDto.getEmail())
                .password(passwordEncoder.encode(profileDto.getPassword()))
                .profileImageUrl(profileDto.getProfileImageUrl())
                .createdAt(profileDto.getCreatedAt())
                .updatedAt(profileDto.getUpdatedAt())
                .build();
    }

    /** Converts entity to DTO, intentionally omitting the password field. */
    public ProfileDto toDto(ProfileEntity profileEntity) {
        return ProfileDto.builder()
                .id(profileEntity.getId())
                .fullName(profileEntity.getFullName())
                .email(profileEntity.getEmail())
                .profileImageUrl(profileEntity.getProfileImageUrl())
                .createdAt(profileEntity.getCreatedAt())
                .updatedAt(profileEntity.getUpdatedAt())
                .build();
    }

    public boolean ActivateProfile(String activationToken) {
        return profileRepository.findByActivationToken(activationToken)
                .map(profile -> {
                    profile.setIsActive(true);
                    profileRepository.save(profile);
                    return true;
                }).orElse(false);
    }

    public boolean isAccountActive(String email) {
        return profileRepository.findByEmail(email)
                .map(ProfileEntity::getIsActive)
                .orElse(false);
    }

    /** Resolves the authenticated user from the Spring Security context. Works for both JWT and session auth. */
    public ProfileEntity getCurrentProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return profileRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Profile not found with email: " + authentication.getName()));
    }

    /** Returns a password-safe DTO. If {@code email} is null, resolves from the current security context. */
    public ProfileDto getPublicProfile(String email) {
        ProfileEntity user = (email == null)
                ? getCurrentProfile()
                : profileRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Profile not found with email: " + email));

        return ProfileDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /** Used by the legacy {@code /login} endpoint. The newer {@code /auth/login} handles auth directly in the controller. */
    public Map<String, Object> authenticateAndGenerateToken(AuthDto authDTO) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authDTO.getEmail(), authDTO.getPassword()));
            return Map.of(
                    "token", jwtUtil.generateToken(authDTO.getEmail()),
                    "user", getPublicProfile(authDTO.getEmail())
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid username and password");
        }
    }
}
