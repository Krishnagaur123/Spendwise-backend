package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.dto.CategoryDto;
import com.krishna.Spendwise.domain.dto.api.ApiCategoryRequest;
import com.krishna.Spendwise.domain.dto.api.ApiCategoryResponse;
import com.krishna.Spendwise.domain.entity.CategoryEntity;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
import com.krishna.Spendwise.exception.BadRequestException;
import com.krishna.Spendwise.exception.ConflictException;
import com.krishna.Spendwise.exception.ForbiddenException;
import com.krishna.Spendwise.exception.ResourceNotFoundException;
import com.krishna.Spendwise.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Category management for both the frontend (legacy DTOs) and the assignment API.
 *
 * <p>Categories are either <b>default</b> (seeded on registration, cannot be deleted) or
 * <b>custom</b> (created by the user). Default categories are flagged with {@code isDefault=true}
 * and protected from deletion in {@link #deleteByName}.
 *
 * <p>Deletion is also blocked if the category is referenced by any existing income or expense records.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProfileService profileService;

    public CategoryDto saveCategory(CategoryDto categoryDTO) {
        ProfileEntity profile = profileService.getCurrentProfile();
        if (categoryRepository.existsByNameAndProfileId(categoryDTO.getName(), profile.getId())) {
            throw new RuntimeException("Category already exists");
        }
        return toDTO(categoryRepository.save(toEntity(categoryDTO, profile)));
    }

    public List<CategoryDto> getCategoriesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        return categoryRepository.findByProfileId(profile.getId()).stream().map(this::toDTO).toList();
    }

    public List<CategoryDto> getCategoriesByTypeForCurrentUser(String type) {
        ProfileEntity profile = profileService.getCurrentProfile();
        return categoryRepository.findByTypeAndProfileId(type, profile.getId()).stream().map(this::toDTO).toList();
    }

    public CategoryDto updateCategory(Long categoryId, CategoryDto dto) {
        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity existing = categoryRepository.findByIdAndProfileId(categoryId, profile.getId())
                .orElseThrow(() -> new RuntimeException("Category not found or not accessible"));
        existing.setName(dto.getName());
        existing.setType(dto.getType());
        existing.setIcon(dto.getIcon());
        return toDTO(categoryRepository.save(existing));
    }

    /**
     * Returns all categories for the current user (defaults + custom), as a flat list.
     * Since defaults are seeded per-user on registration, all categories for this user
     * already live in their profile — no cross-user query needed.
     */
    public List<ApiCategoryResponse> getCategoriesWithDefaults() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<CategoryEntity> all = categoryRepository.findByProfileId(profile.getId());

        List<ApiCategoryResponse> result = new ArrayList<>();
        for (CategoryEntity c : all) {
            result.add(ApiCategoryResponse.builder()
                    .name(c.getName())
                    .type(c.getType())
                    .isCustom(!Boolean.TRUE.equals(c.getIsDefault()))
                    .build());
        }
        return result;
    }


    public ApiCategoryResponse createCustomCategory(ApiCategoryRequest request) {
        ProfileEntity profile = profileService.getCurrentProfile();
        // Check uniqueness within this user's categories (includes their seeded defaults)
        if (categoryRepository.existsByNameAndProfileId(request.getName(), profile.getId())) {
            throw new ConflictException("Category name already exists");
        }
        CategoryEntity entity = CategoryEntity.builder()
                .name(request.getName()).type(request.getType())
                .isDefault(false).profile(profile).build();
        entity = categoryRepository.save(entity);
        return ApiCategoryResponse.builder().name(entity.getName()).type(entity.getType()).isCustom(true).build();
    }

    /**
     * Deletes a user-owned category by name.
     * Guards: (1) cannot delete default categories, (2) cannot delete if referenced by transactions.
     */
    public void deleteByName(String name) {
        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity entity = categoryRepository.findByNameAndProfileId(name, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (Boolean.TRUE.equals(entity.getIsDefault())) {
            throw new ForbiddenException("Cannot delete default category");
        }
        if (categoryRepository.countIncomesByCategory(entity.getId()) > 0 ||
                categoryRepository.countExpensesByCategory(entity.getId()) > 0) {
            throw new BadRequestException("Cannot delete category referenced by transactions");
        }
        categoryRepository.delete(entity);
    }

    public CategoryEntity toEntity(CategoryDto categoryDto, ProfileEntity profileEntity) {
        return CategoryEntity.builder()
                .name(categoryDto.getName()).type(categoryDto.getType())
                .icon(categoryDto.getIcon()).profile(profileEntity).isDefault(false)
                .build();
    }

    private CategoryDto toDTO(CategoryEntity entity) {
        return CategoryDto.builder()
                .id(entity.getId()).name(entity.getName())
                .createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt())
                .type(entity.getType()).icon(entity.getIcon())
                .profileId(entity.getProfile() != null ? entity.getProfile().getId() : null)
                .build();
    }

}
