package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.dto.ExpenseDto;
import com.krishna.Spendwise.domain.dto.IncomeDto;
import com.krishna.Spendwise.domain.dto.RecentTransactionDto;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Aggregates income and expense data for the dashboard summary.
 *
 * <p>The unified {@code recentTransactions} list merges the 5 latest incomes and 5 latest
 * expenses, sorted by date descending. {@code createdAt} is used as a tiebreaker when two
 * entries share the same date. {@link LinkedHashMap} preserves key order in the JSON response.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final ProfileService profileService;

    /**
     * Returns: {@code totalBalance}, {@code totalIncome}, {@code totalExpense},
     * {@code recent5Expenses}, {@code recent5Incomes}, {@code recentTransactions}.
     */
    public Map<String, Object> getDashboardData() {
        ProfileEntity profile = profileService.getCurrentProfile();
        Map<String, Object> data = new LinkedHashMap<>();

        List<IncomeDto> latestIncomes = incomeService.getLatest5IncomesForCurrentUser();
        List<ExpenseDto> latestExpenses = expenseService.getLatest5ExpensesForCurrentUser();

        List<RecentTransactionDto> recent = Stream.concat(
                latestIncomes.stream().map(i -> RecentTransactionDto.builder()
                        .id(i.getId()).profileId(profile.getId()).icon(i.getIcon())
                        .name(i.getName()).amount(i.getAmount()).date(i.getDate())
                        .createdAt(i.getCreatedAt()).updatedAt(i.getUpdatedAt()).type("income").build()),
                latestExpenses.stream().map(e -> RecentTransactionDto.builder()
                        .id(e.getId()).profileId(profile.getId()).icon(e.getIcon())
                        .name(e.getName()).amount(e.getAmount()).date(e.getDate())
                        .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).type("expense").build())
        ).sorted((a, b) -> {
            int cmp = b.getDate().compareTo(a.getDate());
            if (cmp == 0 && a.getCreatedAt() != null && b.getCreatedAt() != null) {
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            }
            return cmp;
        }).collect(Collectors.toList());

        data.put("totalBalance", incomeService.getTotalIncomeForCurrentUser()
                .subtract(expenseService.getTotalExpenseForCurrentUser()));
        data.put("totalIncome", incomeService.getTotalIncomeForCurrentUser());
        data.put("totalExpense", expenseService.getTotalExpenseForCurrentUser());
        data.put("recent5Expenses", latestExpenses);
        data.put("recent5Incomes", latestIncomes);
        data.put("recentTransactions", recent);

        return data;
    }

}
