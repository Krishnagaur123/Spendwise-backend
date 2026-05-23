package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.dto.api.GoalRequest;
import com.krishna.Spendwise.domain.dto.api.GoalResponse;
import com.krishna.Spendwise.domain.dto.api.GoalUpdateRequest;
import com.krishna.Spendwise.domain.entity.GoalEntity;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
import com.krishna.Spendwise.exception.ResourceNotFoundException;
import com.krishna.Spendwise.repository.ExpenseRepository;
import com.krishna.Spendwise.repository.GoalRepository;
import com.krishna.Spendwise.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final ProfileService profileService;

    public GoalResponse createGoal(GoalRequest request) {
        ProfileEntity profile = profileService.getCurrentProfile();
        
        GoalEntity goal = GoalEntity.builder()
                .goalName(request.getGoalName())
                .targetAmount(request.getTargetAmount())
                .targetDate(request.getTargetDate())
                .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now())
                .profile(profile)
                .build();
                
        goal = goalRepository.save(goal);
        return mapToResponse(goal);
    }

    public List<GoalResponse> getAllGoals() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<GoalEntity> goals = goalRepository.findByProfileId(profile.getId());
        return goals.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public GoalResponse getGoalById(Long id) {
        ProfileEntity profile = profileService.getCurrentProfile();
        GoalEntity goal = goalRepository.findByIdAndProfileId(id, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        return mapToResponse(goal);
    }

    public GoalResponse updateGoal(Long id, GoalUpdateRequest request) {
        ProfileEntity profile = profileService.getCurrentProfile();
        GoalEntity goal = goalRepository.findByIdAndProfileId(id, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
                
        goal.setTargetAmount(request.getTargetAmount());
        goal.setTargetDate(request.getTargetDate());
        goal = goalRepository.save(goal);
        return mapToResponse(goal);
    }

    public void deleteGoal(Long id) {
        ProfileEntity profile = profileService.getCurrentProfile();
        GoalEntity goal = goalRepository.findByIdAndProfileId(id, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        goalRepository.delete(goal);
    }

    private GoalResponse mapToResponse(GoalEntity goal) {
        BigDecimal totalIncome = incomeRepository.sumIncomeFromDate(goal.getProfile().getId(), goal.getStartDate());
        BigDecimal totalExpense = expenseRepository.sumExpenseFromDate(goal.getProfile().getId(), goal.getStartDate());
        
        BigDecimal progress = totalIncome.subtract(totalExpense);
        if (progress.compareTo(BigDecimal.ZERO) < 0) {
            progress = BigDecimal.ZERO;
        }

        BigDecimal remaining = goal.getTargetAmount().subtract(progress);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        double percentage = 0.0;
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            percentage = progress.divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).doubleValue();
        }
        
        if (percentage > 100.0) {
            percentage = 100.0;
        }

        return GoalResponse.builder()
                .id(goal.getId())
                .goalName(goal.getGoalName())
                .targetAmount(goal.getTargetAmount())
                .targetDate(goal.getTargetDate())
                .startDate(goal.getStartDate())
                .currentProgress(progress)
                .progressPercentage(percentage)
                .remainingAmount(remaining)
                .build();
    }
}
