package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.dto.api.MonthlyReportResponse;
import com.krishna.Spendwise.domain.dto.api.YearlyReportResponse;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
import com.krishna.Spendwise.repository.ExpenseRepository;
import com.krishna.Spendwise.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates financial reports grouped by category for a given month or year.
 * Aggregation is done at the database level via JPQL GROUP BY queries for efficiency.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final ProfileService profileService;

    public MonthlyReportResponse getMonthlyReport(int year, int month) {
        ProfileEntity profile = profileService.getCurrentProfile();
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();

        Map<String, BigDecimal> incomes = groupedIncomes(profile.getId(), startDate, endDate);
        Map<String, BigDecimal> expenses = groupedExpenses(profile.getId(), startDate, endDate);

        BigDecimal netSavings = sum(incomes).subtract(sum(expenses));

        return MonthlyReportResponse.builder()
                .month(month).year(year)
                .totalIncome(incomes).totalExpenses(expenses).netSavings(netSavings)
                .build();
    }

    public YearlyReportResponse getYearlyReport(int year) {
        ProfileEntity profile = profileService.getCurrentProfile();
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        Map<String, BigDecimal> incomes = groupedIncomes(profile.getId(), startDate, endDate);
        Map<String, BigDecimal> expenses = groupedExpenses(profile.getId(), startDate, endDate);

        BigDecimal netSavings = sum(incomes).subtract(sum(expenses));

        return YearlyReportResponse.builder()
                .year(year)
                .totalIncome(incomes).totalExpenses(expenses).netSavings(netSavings)
                .build();
    }

    private Map<String, BigDecimal> groupedIncomes(Long profileId, LocalDate start, LocalDate end) {
        return toMap(incomeRepository.sumIncomeGroupedByCategoryBetween(profileId, start, end));
    }

    private Map<String, BigDecimal> groupedExpenses(Long profileId, LocalDate start, LocalDate end) {
        return toMap(expenseRepository.sumExpenseGroupedByCategoryBetween(profileId, start, end));
    }

    private Map<String, BigDecimal> toMap(List<Object[]> rows) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((String) row[0], (BigDecimal) row[1]);
        }
        return map;
    }

    private BigDecimal sum(Map<String, BigDecimal> grouped) {
        return grouped.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
