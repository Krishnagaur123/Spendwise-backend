package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.dto.CategoryDto;
import com.krishna.Spendwise.domain.entity.CategoryEntity;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
import com.krishna.Spendwise.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;



import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldSaveCategorySuccessfully() {

        ProfileEntity profile = new ProfileEntity();
        profile.setId(1L);

        CategoryDto dto = CategoryDto.builder()
                .name("Food")
                .type("EXPENSE")
                .icon("🍔")
                .build();

        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(categoryRepository.existsByNameAndProfileId("Food", 1L)).thenReturn(false);

        CategoryEntity saved = CategoryEntity.builder()
                .id(1L)
                .name("Food")
                .type("EXPENSE")
                .profile(profile)
                .build();

        when(categoryRepository.save(any())).thenReturn(saved);

        CategoryDto result = categoryService.saveCategory(dto);

        assertEquals("Food", result.getName());
        verify(categoryRepository).save(any());
    }

    @Test
    void shouldThrowExceptionIfCategoryExists() {

        ProfileEntity profile = new ProfileEntity();
        profile.setId(1L);

        CategoryDto dto = CategoryDto.builder()
                .name("Food")
                .type("EXPENSE")
                .build();

        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(categoryRepository.existsByNameAndProfileId("Food", 1L)).thenReturn(true);

        assertThrows(RuntimeException.class, () ->
                categoryService.saveCategory(dto)
        );
    }

}