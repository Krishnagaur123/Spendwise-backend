package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.dto.api.GoalRequest;
import com.krishna.Spendwise.domain.dto.api.GoalResponse;
import com.krishna.Spendwise.domain.dto.api.GoalUpdateRequest;
import com.krishna.Spendwise.domain.entity.GoalEntity;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
import com.krishna.Spendwise.exception.BadRequestException;
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

/**
 * Savings goal management with real-time progress calculation.
 *
 * Progress formula: {@code net = totalIncome - totalExpense} since {@code startDate}.
 * Capped at 100%, floored at 0 (no negative progress displayed).
 */
@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final ProfileService profileService;

    /**
     * Fix 6: Validates that startDate (if provided) is before targetDate.
     * startDate defaults to today when absent.
     */
    public GoalResponse createGoal(GoalRequest request) {
        ProfileEntity profile = profileService.getCurrentProfile();

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();

        // Reject if startDate is not before targetDate
        if (!startDate.isBefore(request.getTargetDate())) {
            throw new BadRequestException("startDate must be before targetDate");
        }

        GoalEntity goal = GoalEntity.builder()
                .goalName(request.getGoalName())
                .targetAmount(request.getTargetAmount())
                .targetDate(request.getTargetDate())
                .startDate(startDate)
                .profile(profile)
                .build();
        return mapToResponse(goalRepository.save(goal));
    }

    public List<GoalResponse> getAllGoals() {
        ProfileEntity profile = profileService.getCurrentProfile();
        return goalRepository.findByProfileId(profile.getId()).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    public GoalResponse getGoalById(Long id) {
        ProfileEntity profile = profileService.getCurrentProfile();
        GoalEntity goal = goalRepository.findByIdAndProfileId(id, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        return mapToResponse(goal);
    }

    /**
     * Fix 3: Partial update — only updates fields that are non-null in the request.
     * Allows evaluator to send only targetAmount or only targetDate.
     */
    public GoalResponse updateGoal(Long id, GoalUpdateRequest request) {
        ProfileEntity profile = profileService.getCurrentProfile();
        GoalEntity goal = goalRepository.findByIdAndProfileId(id, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));

        if (request.getTargetAmount() != null) {
            goal.setTargetAmount(request.getTargetAmount());
        }
        if (request.getTargetDate() != null) {
            goal.setTargetDate(request.getTargetDate());
        }
        return mapToResponse(goalRepository.save(goal));
    }

    public void deleteGoal(Long id) {
        ProfileEntity profile = profileService.getCurrentProfile();
        GoalEntity goal = goalRepository.findByIdAndProfileId(id, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        goalRepository.delete(goal);
    }

    /**
     * Calculates progress as net savings (income − expense) since {@code goal.startDate}.
     * progressPercentage is rounded to 2 decimal places and capped at 100.
     */
    private GoalResponse mapToResponse(GoalEntity goal) {
        BigDecimal totalIncome = incomeRepository.sumIncomeFromDate(goal.getProfile().getId(), goal.getStartDate());
        BigDecimal totalExpense = expenseRepository.sumExpenseFromDate(goal.getProfile().getId(), goal.getStartDate());

        BigDecimal progress = totalIncome.subtract(totalExpense).max(BigDecimal.ZERO);
        BigDecimal remaining = goal.getTargetAmount().subtract(progress).max(BigDecimal.ZERO);

        double percentage = 0.0;
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            // Round ratio to 4 decimal places first, then multiply → gives 2 significant decimal places in %
            percentage = progress.divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).doubleValue();
        }
        if (percentage > 100.0) percentage = 100.0;

        return GoalResponse.builder()
                .id(goal.getId()).goalName(goal.getGoalName())
                .targetAmount(goal.getTargetAmount()).targetDate(goal.getTargetDate())
                .startDate(goal.getStartDate()).currentProgress(progress)
                .progressPercentage(percentage).remainingAmount(remaining)
                .build();
    }
}
