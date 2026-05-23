package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.entity.ProfileEntity;
import com.krishna.Spendwise.repository.ExpenseRepository;
import com.krishna.Spendwise.repository.IncomeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import com.krishna.Spendwise.domain.dto.api.MonthlyReportResponse;
import com.krishna.Spendwise.domain.dto.api.YearlyReportResponse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ReportService reportService;

    private ProfileEntity profile;

    @BeforeEach
    void setUp() {
        profile = ProfileEntity.builder().id(1L).build();
    }

    @Test
    void getMonthlyReport_shouldAggregateCorrectly() {
        when(profileService.getCurrentProfile()).thenReturn(profile);
        
        Object[] incomeGroup = {"Salary", new BigDecimal("5000")};
        Object[] expenseGroup = {"Food", new BigDecimal("2000")};
        
        when(incomeRepository.sumIncomeGroupedByCategoryBetween(eq(1L), any(), any())).thenReturn(List.<Object[]>of(incomeGroup));
        when(expenseRepository.sumExpenseGroupedByCategoryBetween(eq(1L), any(), any())).thenReturn(List.<Object[]>of(expenseGroup));

        MonthlyReportResponse report = reportService.getMonthlyReport(2023, 5);

        assertThat(report.getTotalIncome().get("Salary")).isEqualTo(new BigDecimal("5000"));
        assertThat(report.getTotalExpenses().get("Food")).isEqualTo(new BigDecimal("2000"));
        assertThat(report.getNetSavings()).isEqualTo(new BigDecimal("3000"));
    }

    @Test
    void getYearlyReport_shouldAggregateCorrectly() {
        when(profileService.getCurrentProfile()).thenReturn(profile);
        
        Object[] incomeGroup = {"Salary", new BigDecimal("60000")};
        Object[] expenseGroup = {"Food", new BigDecimal("24000")};
        
        when(incomeRepository.sumIncomeGroupedByCategoryBetween(eq(1L), any(), any())).thenReturn(List.<Object[]>of(incomeGroup));
        when(expenseRepository.sumExpenseGroupedByCategoryBetween(eq(1L), any(), any())).thenReturn(List.<Object[]>of(expenseGroup));

        YearlyReportResponse report = reportService.getYearlyReport(2023);

        assertThat(report.getTotalIncome().get("Salary")).isEqualTo(new BigDecimal("60000"));
        assertThat(report.getTotalExpenses().get("Food")).isEqualTo(new BigDecimal("24000"));
        assertThat(report.getNetSavings()).isEqualTo(new BigDecimal("36000"));
    }
}
