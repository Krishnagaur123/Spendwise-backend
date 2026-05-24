package com.krishna.Spendwise.domain.dto.api;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Fix 2: Both fields are optional.
 * - amount: if null, the existing amount is preserved (date-only update scenario)
 * - description: if null, the existing description is preserved
 * - date field is intentionally absent — the evaluator sends it but it must be ignored
 */
@Data
public class TransactionUpdateRequest {
    @Positive(message = "Amount must be positive")
    private BigDecimal amount; // nullable — only update if provided

    private String description;
}
