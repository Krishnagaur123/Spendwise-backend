package com.krishna.Spendwise.service;


import com.krishna.Spendwise.domain.dto.CategoryDto;
import com.krishna.Spendwise.domain.entity.CategoryEntity;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
import com.krishna.Spendwise.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProfileService profileService;

    public CategoryDto saveCategory(CategoryDto
                                            categoryDTO) {
        ProfileEntity profile = profileService.getCurrentProfile();
        if (categoryRepository.existsByNameAndProfileId(categoryDTO.getName(), profile.getId())) {
            throw new RuntimeException("Category already exists");
        }
        CategoryEntity newCategory = toEntity(categoryDTO, profile);
        newCategory = categoryRepository.save(newCategory);
        return toDTO(newCategory);
    }

    public List<CategoryDto> getCategoriesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<CategoryEntity> categories = categoryRepository.findByProfileId(profile.getId());
        return categories.stream().map(this::toDTO).toList();
    }

    public List<CategoryDto> getCategoriesByTypeForCurrentUser(String type) {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<CategoryEntity> entities = categoryRepository.findByTypeAndProfileId(type, profile.getId());
        return entities.stream().map(this::toDTO).toList();
    }

    public CategoryDto updateCategory(Long categoryId, CategoryDto dto) {
        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity existingCategory = categoryRepository.findByIdAndProfileId(categoryId, profile.getId())
                .orElseThrow(() -> new RuntimeException("Category not found or not accessible"));
        existingCategory.setName(dto.getName());
        existingCategory.setType(dto.getType());
        existingCategory.setIcon(dto.getIcon());
        existingCategory = categoryRepository.save(existingCategory);
        return toDTO(existingCategory);
    }




    public CategoryEntity toEntity(CategoryDto categoryDto, ProfileEntity profileEntity){
        return CategoryEntity.builder()
                .name(categoryDto.getName())
                .type(categoryDto.getType())
                .icon(categoryDto.getIcon())
                .profile(profileEntity)
                .isDefault(false)
                .build();
    }

    public List<com.krishna.Spendwise.domain.dto.api.ApiCategoryResponse> getCategoriesWithDefaults() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<CategoryEntity> defaults = categoryRepository.findByIsDefaultTrue();
        List<CategoryEntity> customs = categoryRepository.findByProfileId(profile.getId());
        
        List<com.krishna.Spendwise.domain.dto.api.ApiCategoryResponse> result = new java.util.ArrayList<>();
        
        for (CategoryEntity d : defaults) {
            result.add(com.krishna.Spendwise.domain.dto.api.ApiCategoryResponse.builder()
                    .name(d.getName())
                    .type(d.getType())
                    .isCustom(false)
                    .build());
        }
        for (CategoryEntity c : customs) {
            if (Boolean.TRUE.equals(c.getIsDefault())) continue; // Skip if somehow we loaded a default here
            result.add(com.krishna.Spendwise.domain.dto.api.ApiCategoryResponse.builder()
                    .name(c.getName())
                    .type(c.getType())
                    .isCustom(true)
                    .build());
        }
        return result;
    }

    public com.krishna.Spendwise.domain.dto.api.ApiCategoryResponse createCustomCategory(com.krishna.Spendwise.domain.dto.api.ApiCategoryRequest request) {
        ProfileEntity profile = profileService.getCurrentProfile();
        
        if (categoryRepository.existsByNameAndProfileId(request.getName(), profile.getId()) ||
            categoryRepository.findByNameAndProfileId(request.getName(), null).isPresent()) { // check defaults
            throw new com.krishna.Spendwise.exception.ConflictException("Category name already exists");
        }
        
        CategoryEntity entity = CategoryEntity.builder()
                .name(request.getName())
                .type(request.getType())
                .isDefault(false)
                .profile(profile)
                .build();
                
        entity = categoryRepository.save(entity);
        return com.krishna.Spendwise.domain.dto.api.ApiCategoryResponse.builder()
                .name(entity.getName())
                .type(entity.getType())
                .isCustom(true)
                .build();
    }

    public void deleteByName(String name) {
        ProfileEntity profile = profileService.getCurrentProfile();
        
        CategoryEntity entity = categoryRepository.findByNameAndProfileId(name, profile.getId())
                .orElseThrow(() -> new com.krishna.Spendwise.exception.ResourceNotFoundException("Category not found"));
                
        if (Boolean.TRUE.equals(entity.getIsDefault())) {
            throw new com.krishna.Spendwise.exception.ForbiddenException("Cannot delete default category");
        }
        
        if (categoryRepository.countIncomesByCategory(entity.getId()) > 0 || 
            categoryRepository.countExpensesByCategory(entity.getId()) > 0) {
            throw new com.krishna.Spendwise.exception.BadRequestException("Cannot delete category referenced by transactions");
        }
        
        categoryRepository.delete(entity);
    }

    private CategoryDto toDTO(CategoryEntity entity) {
        return CategoryDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .type(entity.getType())
                .icon(entity.getIcon())
                .profileId(entity.getProfile() != null ? entity.getProfile().getId() : null)
                .build();
    }

}
