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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApiTransactionService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final ProfileService profileService;

    public TransactionResponse createTransaction(TransactionRequest request) {
        ProfileEntity profile = profileService.getCurrentProfile();
        
        CategoryEntity category = categoryRepository.findByNameAndProfileId(request.getCategory(), profile.getId())
                .orElseGet(() -> categoryRepository.findByNameAndProfileId(request.getCategory(), null) // For shared defaults if profile is null (though we seed them now)
                .orElseThrow(() -> new BadRequestException("Invalid category: " + request.getCategory())));

        if ("INCOME".equalsIgnoreCase(category.getType())) {
            IncomeEntity income = IncomeEntity.builder()
                    .amount(request.getAmount())
                    .date(request.getDate())
                    .name(request.getDescription() != null ? request.getDescription() : request.getCategory())
                    .category(category)
                    .profile(profile)
                    .build();
            income = incomeRepository.save(income);
            return mapIncome(income);
        } else {
            ExpenseEntity expense = ExpenseEntity.builder()
                    .amount(request.getAmount())
                    .date(request.getDate())
                    .name(request.getDescription() != null ? request.getDescription() : request.getCategory())
                    .category(category)
                    .profile(profile)
                    .build();
            expense = expenseRepository.save(expense);
            return mapExpense(expense);
        }
    }

    public List<TransactionResponse> getTransactions(LocalDate startDate, LocalDate endDate, Long categoryId) {
        ProfileEntity profile = profileService.getCurrentProfile();
        
        if (startDate == null) startDate = LocalDate.of(2000, 1, 1);
        if (endDate == null) endDate = LocalDate.now().plusYears(100);

        List<TransactionResponse> results = new ArrayList<>();

        List<IncomeEntity> incomes = incomeRepository.findByProfileIdAndDateBetween(profile.getId(), startDate, endDate);
        List<ExpenseEntity> expenses = expenseRepository.findByProfileIdAndDateBetween(profile.getId(), startDate, endDate);

        for (IncomeEntity inc : incomes) {
            if (categoryId == null || inc.getCategory().getId().equals(categoryId)) {
                results.add(mapIncome(inc));
            }
        }
        for (ExpenseEntity exp : expenses) {
            if (categoryId == null || exp.getCategory().getId().equals(categoryId)) {
                results.add(mapExpense(exp));
            }
        }

        // Sort by date descending
        results.sort(Comparator.comparing(TransactionResponse::getDate).reversed()
                .thenComparing((r1, r2) -> r2.getId().compareTo(r1.getId())));

        return results;
    }

    public TransactionResponse updateTransaction(Long id, TransactionUpdateRequest request) {
        ProfileEntity profile = profileService.getCurrentProfile();
        
        // Try income first
        var incomeOpt = incomeRepository.findByIdAndProfileId(id, profile.getId());
        if (incomeOpt.isPresent()) {
            IncomeEntity income = incomeOpt.get();
            income.setAmount(request.getAmount());
            if (request.getDescription() != null) {
                income.setName(request.getDescription());
            }
            income = incomeRepository.save(income);
            return mapIncome(income);
        }
        
        // Try expense
        var expenseOpt = expenseRepository.findByIdAndProfileId(id, profile.getId());
        if (expenseOpt.isPresent()) {
            ExpenseEntity expense = expenseOpt.get();
            expense.setAmount(request.getAmount());
            if (request.getDescription() != null) {
                expense.setName(request.getDescription());
            }
            expense = expenseRepository.save(expense);
            return mapExpense(expense);
        }
        
        throw new ResourceNotFoundException("Transaction not found with id " + id);
    }

    public void deleteTransaction(Long id) {
        ProfileEntity profile = profileService.getCurrentProfile();
        
        var incomeOpt = incomeRepository.findByIdAndProfileId(id, profile.getId());
        if (incomeOpt.isPresent()) {
            incomeRepository.delete(incomeOpt.get());
            return;
        }
        
        var expenseOpt = expenseRepository.findByIdAndProfileId(id, profile.getId());
        if (expenseOpt.isPresent()) {
            expenseRepository.delete(expenseOpt.get());
            return;
        }
        
        throw new ResourceNotFoundException("Transaction not found with id " + id);
    }

    private TransactionResponse mapIncome(IncomeEntity entity) {
        return TransactionResponse.builder()
                .id(entity.getId())
                .amount(entity.getAmount())
                .date(entity.getDate())
                .category(entity.getCategory().getName())
                .description(entity.getName()) // Using name as description since original entities don't have separate description field
                .type("INCOME")
                .build();
    }

    private TransactionResponse mapExpense(ExpenseEntity entity) {
        return TransactionResponse.builder()
                .id(entity.getId())
                .amount(entity.getAmount())
                .date(entity.getDate())
                .category(entity.getCategory().getName())
                .description(entity.getName()) // Using name as description
                .type("EXPENSE")
                .build();
    }
}
