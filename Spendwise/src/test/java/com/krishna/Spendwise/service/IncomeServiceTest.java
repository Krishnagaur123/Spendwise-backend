package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.dto.IncomeDto;
import com.krishna.Spendwise.domain.entity.CategoryEntity;
import com.krishna.Spendwise.domain.entity.IncomeEntity;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
import com.krishna.Spendwise.repository.CategoryRepository;
import com.krishna.Spendwise.repository.IncomeRepository;

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
class IncomeServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private IncomeService incomeService;

    private ProfileEntity profile;
    private CategoryEntity category;

    @BeforeEach
    void setup() {

        profile = ProfileEntity.builder()
                .id(1L)
                .build();

        category = CategoryEntity.builder()
                .id(20L)
                .name("Salary")
                .build();
    }

    @Test
    void addIncome_shouldSaveIncome() {

        IncomeDto dto = IncomeDto.builder()
                .name("Salary")
                .amount(new BigDecimal("50000"))
                .categoryId(20L)
                .date(LocalDate.now())
                .build();

        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));
        when(incomeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        IncomeDto result = incomeService.addIncome(dto);

        assertThat(result.getName()).isEqualTo("Salary");
        verify(incomeRepository).save(any());
    }

    @Test
    void getCurrentMonthIncomes_shouldReturnList() {

        IncomeEntity entity = IncomeEntity.builder()
                .id(1L)
                .name("Bonus")
                .amount(new BigDecimal("10000"))
                .date(LocalDate.now())
                .profile(profile)
                .category(category)
                .build();

        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(incomeRepository.findByProfileIdAndDateBetween(any(), any(), any()))
                .thenReturn(List.of(entity));

        List<IncomeDto> result = incomeService.getCurrentMonthIncomesForCurrentUser();

        assertThat(result).hasSize(1);
    }


}
