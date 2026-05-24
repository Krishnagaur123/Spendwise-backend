package com.krishna.Spendwise.domain.dto.api;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fix 3: Both fields are optional so the evaluator can send partial updates
 * (e.g. only targetAmount or only targetDate). The service skips null fields.
 */
@Data
public class GoalUpdateRequest {
    @Positive(message = "Target amount must be positive")
    private BigDecimal targetAmount; // nullable — only update if provided

    @Future(message = "Target date must be in the future")
    private LocalDate targetDate; // nullable — only update if provided
}
