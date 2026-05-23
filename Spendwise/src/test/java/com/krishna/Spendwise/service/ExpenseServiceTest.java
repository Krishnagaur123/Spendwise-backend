package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.dto.ExpenseDto;
import com.krishna.Spendwise.domain.entity.CategoryEntity;
import com.krishna.Spendwise.domain.entity.ExpenseEntity;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
import com.krishna.Spendwise.repository.CategoryRepository;
import com.krishna.Spendwise.repository.ExpenseRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {


    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ExpenseService expenseService;

    private ProfileEntity profile;
    private CategoryEntity category;

    @BeforeEach
    void setup() {

        profile = ProfileEntity.builder()
                .id(1L)
                .build();

        category = CategoryEntity.builder()
                .id(10L)
                .name("Food")
                .build();
    }

    @Test
    void addExpense_shouldSaveExpense() {

        ExpenseDto dto = ExpenseDto.builder()
                .name("Lunch")
                .amount(new BigDecimal("200"))
                .categoryId(10L)
                .date(LocalDate.now())
                .build();

        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(expenseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ExpenseDto result = expenseService.addExpense(dto);

        assertThat(result.getName()).isEqualTo("Lunch");
        verify(expenseRepository).save(any());
    }

    @Test
    void getCurrentMonthExpenses_shouldReturnList() {

        ExpenseEntity entity = ExpenseEntity.builder()
                .id(1L)
                .name("Dinner")
                .amount(new BigDecimal("300"))
                .date(LocalDate.now())
                .profile(profile)
                .category(category)
                .build();

        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(expenseRepository.findByProfileIdAndDateBetween(any(), any(), any()))
                .thenReturn(List.of(entity));

        List<ExpenseDto> result = expenseService.getCurrentMonthExpensesForCurrentUser();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Dinner");
    }

    @Test
    void deleteExpense_shouldDeleteWhenAuthorized() {

        ExpenseEntity entity = ExpenseEntity.builder()
                .id(1L)
                .profile(profile)
                .build();

        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(entity));

        expenseService.deleteExpense(1L);

        verify(expenseRepository).delete(entity);
    }

    @Test
    void deleteExpense_shouldThrowIfUnauthorized() {

        ProfileEntity otherProfile = ProfileEntity.builder().id(2L).build();

        ExpenseEntity entity = ExpenseEntity.builder()
                .id(1L)
                .profile(otherProfile)
                .build();

        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> expenseService.deleteExpense(1L))
                .isInstanceOf(RuntimeException.class);
    }


}
