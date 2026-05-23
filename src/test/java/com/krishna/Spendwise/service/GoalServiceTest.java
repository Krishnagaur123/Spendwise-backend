package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.dto.api.GoalRequest;
import com.krishna.Spendwise.domain.dto.api.GoalResponse;
import com.krishna.Spendwise.domain.dto.api.GoalUpdateRequest;
import com.krishna.Spendwise.domain.entity.GoalEntity;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
import com.krishna.Spendwise.repository.ExpenseRepository;
import com.krishna.Spendwise.repository.GoalRepository;
import com.krishna.Spendwise.repository.IncomeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private GoalService goalService;

    private ProfileEntity profile;

    @BeforeEach
    void setUp() {
        profile = ProfileEntity.builder().id(1L).build();
    }

    @Test
    void addGoal_shouldSaveAndReturn() {
        GoalRequest request = new GoalRequest();
        request.setGoalName("Test Goal");
        request.setTargetAmount(new BigDecimal("1000"));
        request.setTargetDate(LocalDate.now().plusMonths(1));

        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(incomeRepository.sumIncomeFromDate(any(), any())).thenReturn(BigDecimal.ZERO);
        when(expenseRepository.sumExpenseFromDate(any(), any())).thenReturn(BigDecimal.ZERO);
        
        when(goalRepository.save(any())).thenAnswer(i -> {
            GoalEntity saved = i.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        GoalResponse response = goalService.createGoal(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getGoalName()).isEqualTo("Test Goal");
        verify(goalRepository).save(any());
    }

    @Test
    void getAllGoals_shouldCalculateProgress() {
        GoalEntity goal = GoalEntity.builder()
                .id(1L)
                .goalName("Car")
                .targetAmount(new BigDecimal("5000"))
                .startDate(LocalDate.now().minusDays(10))
                .targetDate(LocalDate.now().plusDays(20))
                .profile(profile)
                .build();

        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(goalRepository.findByProfileId(1L)).thenReturn(List.of(goal));
        // Simulate income = 3000, expense = 1000 since start date. Progress = 2000.
        when(incomeRepository.sumIncomeFromDate(eq(1L), any())).thenReturn(new BigDecimal("3000"));
        when(expenseRepository.sumExpenseFromDate(eq(1L), any())).thenReturn(new BigDecimal("1000"));

        List<GoalResponse> results = goalService.getAllGoals();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCurrentProgress()).isEqualTo(new BigDecimal("2000"));
    }

    @Test
    void updateGoal_shouldUpdateFields() {
        GoalEntity goal = GoalEntity.builder().id(1L).profile(profile).build();
        GoalUpdateRequest request = new GoalUpdateRequest();
        request.setTargetAmount(new BigDecimal("2000"));
        request.setTargetAmount(new BigDecimal("2000"));
        request.setTargetDate(LocalDate.now().plusMonths(2));

        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(incomeRepository.sumIncomeFromDate(any(), any())).thenReturn(BigDecimal.ZERO);
        when(expenseRepository.sumExpenseFromDate(any(), any())).thenReturn(BigDecimal.ZERO);
        
        when(goalRepository.findByIdAndProfileId(1L, 1L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GoalResponse response = goalService.updateGoal(1L, request);

        assertThat(response.getTargetAmount()).isEqualTo(new BigDecimal("2000"));
        verify(goalRepository).save(any());
    }

    @Test
    void deleteGoal_shouldDelete() {
        GoalEntity goal = GoalEntity.builder().id(1L).profile(profile).build();
        
        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(goalRepository.findByIdAndProfileId(1L, 1L)).thenReturn(Optional.of(goal));

        goalService.deleteGoal(1L);

        verify(goalRepository).delete(goal);
    }
}
