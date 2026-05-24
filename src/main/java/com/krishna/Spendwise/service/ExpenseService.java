package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.dto.ExpenseDto;
import com.krishna.Spendwise.domain.entity.CategoryEntity;
import com.krishna.Spendwise.domain.entity.ExpenseEntity;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
import com.krishna.Spendwise.repository.CategoryRepository;
import com.krishna.Spendwise.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for expense records.
 * All user-scoped queries resolve the profile from the Spring Security context —
 * controllers never need to pass a user ID.
 */
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final ProfileService profileService;

    public ExpenseDto addExpense(ExpenseDto dto) {
        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return toDTO(expenseRepository.save(toEntity(dto, profile, category)));
    }

    /** Returns expenses for the current calendar month. Default view on the Expense page. */
    public List<ExpenseDto> getCurrentMonthExpensesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        LocalDate now = LocalDate.now();
        return expenseRepository
                .findByProfileIdAndDateBetween(profile.getId(), now.withDayOfMonth(1), now.withDayOfMonth(now.lengthOfMonth()))
                .stream().map(this::toDTO).toList();
    }

    /** Enforces ownership — only the expense owner can delete it. */
    public void deleteExpense(Long expenseId) {
        ProfileEntity profile = profileService.getCurrentProfile();
        ExpenseEntity entity = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        if (!entity.getProfile().getId().equals(profile.getId())) {
            throw new RuntimeException("Unauthorized to delete this expense");
        }
        expenseRepository.delete(entity);
    }

    /** Top 5 by date — used in the dashboard recent transactions widget. */
    public List<ExpenseDto> getLatest5ExpensesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        return expenseRepository.findTop5ByProfileIdOrderByDateDesc(profile.getId())
                .stream().map(this::toDTO).toList();
    }

    /** All-time total. Returns ZERO instead of null when no records exist. */
    public BigDecimal getTotalExpenseForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        BigDecimal total = expenseRepository.findTotalExpenseByProfileId(profile.getId());
        return total != null ? total : BigDecimal.ZERO;
    }

    /** Used by the Filter page — supports date range, keyword, and sort direction. */
    public List<ExpenseDto> filterExpenses(LocalDate startDate, LocalDate endDate, String keyword, Sort sort) {
        ProfileEntity profile = profileService.getCurrentProfile();
        return expenseRepository.findByProfileIdAndDateBetweenAndNameContainingIgnoreCase(
                profile.getId(), startDate, endDate, keyword, sort)
                .stream().map(this::toDTO).toList();
    }

    /** Called by {@link NotificationService} to build the nightly expense summary email. */
    public List<ExpenseDto> getExpenseForUserOnDate(Long profileId, LocalDate date) {
        return expenseRepository.findByProfileIdAndDate(profileId, date)
                .stream().map(this::toDTO).toList();
    }

    private ExpenseEntity toEntity(ExpenseDto dto, ProfileEntity profile, CategoryEntity category) {
        return ExpenseEntity.builder()
                .name(dto.getName()).icon(dto.getIcon())
                .amount(dto.getAmount()).date(dto.getDate())
                .profile(profile).category(category)
                .build();
    }

    private ExpenseDto toDTO(ExpenseEntity entity) {
        return ExpenseDto.builder()
                .id(entity.getId()).name(entity.getName()).icon(entity.getIcon())
                .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : "N/A")
                .amount(entity.getAmount()).date(entity.getDate())
                .createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt())
                .build();
    }

}
