package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.dto.api.TransactionRequest;
import com.krishna.Spendwise.domain.dto.api.TransactionResponse;
import com.krishna.Spendwise.domain.dto.api.TransactionUpdateRequest;
import com.krishna.Spendwise.domain.entity.CategoryEntity;
import com.krishna.Spendwise.domain.entity.ExpenseEntity;
import com.krishna.Spendwise.domain.entity.IncomeEntity;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
import com.krishna.Spendwise.repository.CategoryRepository;
import com.krishna.Spendwise.repository.ExpenseRepository;
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
class ApiTransactionServiceTest {

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ApiTransactionService apiTransactionService;

    private ProfileEntity profile;

    @BeforeEach
    void setUp() {
        profile = ProfileEntity.builder().id(1L).build();
    }

    @Test
    void addTransaction_whenIncome_shouldSaveIncome() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("100"));
        request.setCategory("Salary");
        request.setDate(LocalDate.now());

        CategoryEntity category = CategoryEntity.builder().id(1L).type("INCOME").build();

        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(categoryRepository.findByNameAndProfileId("Salary", 1L)).thenReturn(Optional.of(category));
        when(incomeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TransactionResponse response = apiTransactionService.createTransaction(request);

        assertThat(response.getType()).isEqualTo("INCOME");
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("100"));
        verify(incomeRepository).save(any());
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void addTransaction_whenExpense_shouldSaveExpense() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("100"));
        request.setCategory("Salary");
        request.setDate(LocalDate.now());

        CategoryEntity category = CategoryEntity.builder().id(1L).type("EXPENSE").build();

        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(categoryRepository.findByNameAndProfileId("Salary", 1L)).thenReturn(Optional.of(category));
        when(expenseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TransactionResponse response = apiTransactionService.createTransaction(request);

        assertThat(response.getType()).isEqualTo("EXPENSE");
        verify(expenseRepository).save(any());
        verify(incomeRepository, never()).save(any());
    }

    @Test
    void getAllTransactions_shouldMergeAndSort() {
        IncomeEntity income = IncomeEntity.builder()
                .id(1L)
                .amount(new BigDecimal("100"))
                .date(LocalDate.now().minusDays(1))
                .category(CategoryEntity.builder().type("INCOME").build())
                .build();
        
        ExpenseEntity expense = ExpenseEntity.builder()
                .id(2L)
                .amount(new BigDecimal("50"))
                .date(LocalDate.now())
                .category(CategoryEntity.builder().type("EXPENSE").build())
                .build();

        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(incomeRepository.findByProfileIdAndDateBetween(eq(1L), any(), any())).thenReturn(List.of(income));
        when(expenseRepository.findByProfileIdAndDateBetween(eq(1L), any(), any())).thenReturn(List.of(expense));

        List<TransactionResponse> result = apiTransactionService.getTransactions(null, null, null);

        assertThat(result).hasSize(2);
        // Should be sorted by date descending, so expense (now) comes before income (yesterday)
        assertThat(result.get(0).getType()).isEqualTo("EXPENSE");
        assertThat(result.get(1).getType()).isEqualTo("INCOME");
    }

    @Test
    void updateTransaction_whenIncome_shouldUpdate() {
        IncomeEntity income = IncomeEntity.builder()
                .id(1L)
                .amount(new BigDecimal("100"))
                .category(CategoryEntity.builder().type("INCOME").build())
                .build();

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setAmount(new BigDecimal("200"));
        request.setDescription("Updated");

        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(incomeRepository.findByIdAndProfileId(1L, 1L)).thenReturn(Optional.of(income));
        when(incomeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TransactionResponse response = apiTransactionService.updateTransaction(1L, request);

        assertThat(response.getAmount()).isEqualTo(new BigDecimal("200"));
        assertThat(response.getDescription()).isEqualTo("Updated");
        verify(incomeRepository).save(any());
    }

    @Test
    void updateTransaction_whenExpense_shouldUpdate() {
        ExpenseEntity expense = ExpenseEntity.builder()
                .id(1L)
                .amount(new BigDecimal("100"))
                .category(CategoryEntity.builder().type("EXPENSE").build())
                .build();

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setAmount(new BigDecimal("200"));
        request.setDescription("Updated");

        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(incomeRepository.findByIdAndProfileId(1L, 1L)).thenReturn(Optional.empty());
        when(expenseRepository.findByIdAndProfileId(1L, 1L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TransactionResponse response = apiTransactionService.updateTransaction(1L, request);

        assertThat(response.getAmount()).isEqualTo(new BigDecimal("200"));
        assertThat(response.getDescription()).isEqualTo("Updated");
        verify(expenseRepository).save(any());
    }

    @Test
    void deleteTransaction_whenIncome_shouldDelete() {
        IncomeEntity income = IncomeEntity.builder().id(1L).build();
        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(incomeRepository.findByIdAndProfileId(1L, 1L)).thenReturn(Optional.of(income));

        apiTransactionService.deleteTransaction(1L);

        verify(incomeRepository).delete(income);
        verify(expenseRepository, never()).delete(any());
    }

    @Test
    void deleteTransaction_whenExpense_shouldDelete() {
        ExpenseEntity expense = ExpenseEntity.builder().id(1L).build();
        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(incomeRepository.findByIdAndProfileId(1L, 1L)).thenReturn(Optional.empty());
        when(expenseRepository.findByIdAndProfileId(1L, 1L)).thenReturn(Optional.of(expense));

        apiTransactionService.deleteTransaction(1L);

        verify(expenseRepository).delete(expense);
    }
}
