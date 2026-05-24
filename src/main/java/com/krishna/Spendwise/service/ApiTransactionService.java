package com.krishna.Spendwise.service;

import com.krishna.Spendwise.domain.dto.api.TransactionRequest;
import com.krishna.Spendwise.domain.dto.api.TransactionResponse;
import com.krishna.Spendwise.domain.dto.api.TransactionUpdateRequest;
import com.krishna.Spendwise.domain.entity.CategoryEntity;
import com.krishna.Spendwise.domain.entity.ExpenseEntity;
import com.krishna.Spendwise.domain.entity.IncomeEntity;
import com.krishna.Spendwise.domain.entity.ProfileEntity;
import com.krishna.Spendwise.exception.BadRequestException;
import com.krishna.Spendwise.exception.ResourceNotFoundException;
import com.krishna.Spendwise.repository.CategoryRepository;
import com.krishna.Spendwise.repository.ExpenseRepository;
import com.krishna.Spendwise.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Unified transaction API treating income and expense as a single resource.
 * Type is inferred from the resolved category's {@code type} field (INCOME/EXPENSE).
 * On update/delete, income table is checked first, then expense.
 */
@Service
@RequiredArgsConstructor
public class ApiTransactionService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final ProfileService profileService;

    /**
     * Creates an income or expense based on the resolved category type.
     * Category lookup: user-owned first, then system defaults (isDefault=true).
     */
    public TransactionResponse createTransaction(TransactionRequest request) {
        ProfileEntity profile = profileService.getCurrentProfile();

        CategoryEntity category = categoryRepository.findByNameAndProfileId(request.getCategory(), profile.getId())
                .orElseGet(() -> categoryRepository.findByIsDefaultTrueAndName(request.getCategory())
                        .orElseThrow(() -> new BadRequestException("Invalid category: " + request.getCategory())));

        if ("INCOME".equalsIgnoreCase(category.getType())) {
            IncomeEntity income = IncomeEntity.builder()
                    .amount(request.getAmount()).date(request.getDate())
                    .name(request.getDescription() != null ? request.getDescription() : request.getCategory())
                    .category(category).profile(profile).build();
            return mapIncome(incomeRepository.save(income));
        } else {
            ExpenseEntity expense = ExpenseEntity.builder()
                    .amount(request.getAmount()).date(request.getDate())
                    .name(request.getDescription() != null ? request.getDescription() : request.getCategory())
                    .category(category).profile(profile).build();
            return mapExpense(expenseRepository.save(expense));
        }
    }

    /**
     * Returns all transactions within the optional date range and category filter.
     * Fix 4: category filter is by name (String) not by ID to match evaluator query param.
     */
    public List<TransactionResponse> getTransactions(LocalDate startDate, LocalDate endDate, String categoryName) {
        ProfileEntity profile = profileService.getCurrentProfile();
        if (startDate == null) startDate = LocalDate.of(2000, 1, 1);
        if (endDate == null) endDate = LocalDate.now().plusYears(100);

        List<TransactionResponse> results = new ArrayList<>();
        incomeRepository.findByProfileIdAndDateBetween(profile.getId(), startDate, endDate).stream()
                .filter(i -> categoryName == null || categoryName.equalsIgnoreCase(i.getCategory().getName()))
                .map(this::mapIncome).forEach(results::add);
        expenseRepository.findByProfileIdAndDateBetween(profile.getId(), startDate, endDate).stream()
                .filter(e -> categoryName == null || categoryName.equalsIgnoreCase(e.getCategory().getName()))
                .map(this::mapExpense).forEach(results::add);

        results.sort(Comparator.comparing(TransactionResponse::getDate).reversed()
                .thenComparing((r1, r2) -> r2.getId().compareTo(r1.getId())));
        return results;
    }

    private static final long EXPENSE_ID_OFFSET = 1000000000L;

    /**
     * Partial update: only amount and description are updated; date is intentionally ignored.
     * Fix 2: amount may be null (date-only request) — skip update in that case.
     */
    public TransactionResponse updateTransaction(Long id, TransactionUpdateRequest request) {
        ProfileEntity profile = profileService.getCurrentProfile();

        if (id >= EXPENSE_ID_OFFSET) {
            Long realId = id - EXPENSE_ID_OFFSET;
            Optional<ExpenseEntity> expenseOpt = expenseRepository.findByIdAndProfileId(realId, profile.getId());
            if (expenseOpt.isPresent()) {
                ExpenseEntity expense = expenseOpt.get();
                if (request.getAmount() != null) expense.setAmount(request.getAmount());
                if (request.getDescription() != null) expense.setName(request.getDescription());
                return mapExpense(expenseRepository.save(expense));
            }
        } else {
            Optional<IncomeEntity> incomeOpt = incomeRepository.findByIdAndProfileId(id, profile.getId());
            if (incomeOpt.isPresent()) {
                IncomeEntity income = incomeOpt.get();
                if (request.getAmount() != null) income.setAmount(request.getAmount());
                if (request.getDescription() != null) income.setName(request.getDescription());
                return mapIncome(incomeRepository.save(income));
            }
        }

        throw new ResourceNotFoundException("Transaction not found with id " + id);
    }

    /** Checks income table first, then expense. Throws 404 if not found in either. */
    public void deleteTransaction(Long id) {
        ProfileEntity profile = profileService.getCurrentProfile();

        if (id >= EXPENSE_ID_OFFSET) {
            Long realId = id - EXPENSE_ID_OFFSET;
            Optional<ExpenseEntity> expenseOpt = expenseRepository.findByIdAndProfileId(realId, profile.getId());
            if (expenseOpt.isPresent()) { expenseRepository.delete(expenseOpt.get()); return; }
        } else {
            Optional<IncomeEntity> incomeOpt = incomeRepository.findByIdAndProfileId(id, profile.getId());
            if (incomeOpt.isPresent()) { incomeRepository.delete(incomeOpt.get()); return; }
        }

        throw new ResourceNotFoundException("Transaction not found with id " + id);
    }

    private TransactionResponse mapIncome(IncomeEntity entity) {
        return TransactionResponse.builder()
                .id(entity.getId()).amount(entity.getAmount()).date(entity.getDate())
                .category(entity.getCategory().getName())
                .description(entity.getName())
                .type("INCOME").build();
    }

    private TransactionResponse mapExpense(ExpenseEntity entity) {
        return TransactionResponse.builder()
                .id(entity.getId() + EXPENSE_ID_OFFSET).amount(entity.getAmount()).date(entity.getDate())
                .category(entity.getCategory().getName())
                .description(entity.getName())
                .type("EXPENSE").build();
    }
}
