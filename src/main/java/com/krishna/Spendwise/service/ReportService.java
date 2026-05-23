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

        Map<String, BigDecimal> incomes = getGroupedIncomes(profile.getId(), startDate, endDate);
        Map<String, BigDecimal> expenses = getGroupedExpenses(profile.getId(), startDate, endDate);

        BigDecimal totalIncome = incomes.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = expenses.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netSavings = totalIncome.subtract(totalExpense);

        return MonthlyReportResponse.builder()
                .month(month)
                .year(year)
                .totalIncome(incomes)
                .totalExpenses(expenses)
                .netSavings(netSavings)
                .build();
    }

    public YearlyReportResponse getYearlyReport(int year) {
        ProfileEntity profile = profileService.getCurrentProfile();
        
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        Map<String, BigDecimal> incomes = getGroupedIncomes(profile.getId(), startDate, endDate);
        Map<String, BigDecimal> expenses = getGroupedExpenses(profile.getId(), startDate, endDate);

        BigDecimal totalIncome = incomes.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = expenses.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netSavings = totalIncome.subtract(totalExpense);

        return YearlyReportResponse.builder()
                .year(year)
                .totalIncome(incomes)
                .totalExpenses(expenses)
                .netSavings(netSavings)
                .build();
    }

    private Map<String, BigDecimal> getGroupedIncomes(Long profileId, LocalDate start, LocalDate end) {
        List<Object[]> results = incomeRepository.sumIncomeGroupedByCategoryBetween(profileId, start, end);
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] row : results) {
            String category = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            map.put(category, amount);
        }
        return map;
    }

    private Map<String, BigDecimal> getGroupedExpenses(Long profileId, LocalDate start, LocalDate end) {
        List<Object[]> results = expenseRepository.sumExpenseGroupedByCategoryBetween(profileId, start, end);
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] row : results) {
            String category = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            map.put(category, amount);
        }
        return map;
    }
}
