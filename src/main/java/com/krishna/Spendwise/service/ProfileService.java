package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.dto.AuthDto;
import com.krishna.Spendwise.domain.dto.ProfileDto;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
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

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final com.krishna.Spendwise.repository.CategoryRepository categoryRepository;

    @Value("${app.activation.url}")
    private String activationUrl;

    public ProfileDto registerProfile(ProfileDto profileDto) {
        ProfileEntity newProfile = toEntity(profileDto);
        newProfile.setActivationToken(UUID.randomUUID().toString());
        ProfileEntity newEntity=profileRepository.save(newProfile);

        // Seed default categories for this new user
        seedDefaultCategories(newEntity);

        //verifying email
        String activationLink=activationUrl+"/api/v1.0/activate?token="+newProfile.getActivationToken();
        String subject = "Activate Profile";
        String body="click here to activate your profile:"+activationLink;
        try {
            emailService.sendVerificationEmail(newProfile.getEmail(),subject,body);
        } catch (Exception e) {
            // Ignore email errors if mail is disabled
        }

        return toDto(newEntity);
    }

    public Long registerWithoutActivation(com.krishna.Spendwise.domain.dto.api.RegisterRequest request) {
        if (profileRepository.findByEmail(request.getUsername()).isPresent()) {
            throw new com.krishna.Spendwise.exception.ConflictException("Email is already in use");
        }
        ProfileEntity newProfile = ProfileEntity.builder()
                .email(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .isActive(true) // Assignment requires immediate login
                .build();
        newProfile = profileRepository.save(newProfile);
        
        // Seed default categories for this new user
        seedDefaultCategories(newProfile);
        
        return newProfile.getId();
    }
    
    private void seedDefaultCategories(ProfileEntity profile) {
        // Income
        com.krishna.Spendwise.domain.entity.CategoryEntity salary = com.krishna.Spendwise.domain.entity.CategoryEntity.builder()
                .name("Salary").type("INCOME").isDefault(true).profile(profile).build();
                
        // Expenses
        com.krishna.Spendwise.domain.entity.CategoryEntity food = com.krishna.Spendwise.domain.entity.CategoryEntity.builder()
                .name("Food").type("EXPENSE").isDefault(true).profile(profile).build();
        com.krishna.Spendwise.domain.entity.CategoryEntity rent = com.krishna.Spendwise.domain.entity.CategoryEntity.builder()
                .name("Rent").type("EXPENSE").isDefault(true).profile(profile).build();
        com.krishna.Spendwise.domain.entity.CategoryEntity transport = com.krishna.Spendwise.domain.entity.CategoryEntity.builder()
                .name("Transportation").type("EXPENSE").isDefault(true).profile(profile).build();
        com.krishna.Spendwise.domain.entity.CategoryEntity entertain = com.krishna.Spendwise.domain.entity.CategoryEntity.builder()
                .name("Entertainment").type("EXPENSE").isDefault(true).profile(profile).build();
        com.krishna.Spendwise.domain.entity.CategoryEntity health = com.krishna.Spendwise.domain.entity.CategoryEntity.builder()
                .name("Healthcare").type("EXPENSE").isDefault(true).profile(profile).build();
        com.krishna.Spendwise.domain.entity.CategoryEntity utilities = com.krishna.Spendwise.domain.entity.CategoryEntity.builder()
                .name("Utilities").type("EXPENSE").isDefault(true).profile(profile).build();
                
        // Save them
        categoryRepository.saveAll(java.util.List.of(salary, food, rent, transport, entertain, health, utilities));
    }

    public ProfileEntity toEntity(ProfileDto  profileDto) {
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
    public ProfileDto toDto(ProfileEntity  profileEntity) {
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
                .map(profile->{
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

    public ProfileEntity getCurrentProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return profileRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Profile not found with email: " + authentication.getName()));
    }

    public ProfileDto getPublicProfile(String email) {
        ProfileEntity currentUser = null;
        if (email == null) {
            currentUser = getCurrentProfile();
        } else {
            currentUser = profileRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Profile not found with email: " + email));
        }
        return ProfileDto.builder()
                .id(currentUser.getId())
                .fullName(currentUser.getFullName())
                .email(currentUser.getEmail())
                .profileImageUrl(currentUser.getProfileImageUrl())
                .createdAt(currentUser.getCreatedAt())
                .updatedAt(currentUser.getUpdatedAt())
                .build();

    }

    public Map<String, Object> authenticateAndGenerateToken(AuthDto authDTO) {
        try{
            authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(authDTO.getEmail(), authDTO.getPassword()));
            String token = jwtUtil.generateToken(authDTO.getEmail());
            return Map.of(
                    "token",token,
                    "user",getPublicProfile(authDTO.getEmail())
                    );
        }catch (Exception e){
            throw new RuntimeException("Invalid username and password");
        }
    }
}
