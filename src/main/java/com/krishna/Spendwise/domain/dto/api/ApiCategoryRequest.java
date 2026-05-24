package com.krishna.Spendwise.domain.dto.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Fix 7: Validate that type is exactly "INCOME" or "EXPENSE" (case-sensitive).
 * Any other value returns 400 via @Valid on the controller.
 */
@Data
public class ApiCategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;

    @NotBlank(message = "Category type is required")
    @Pattern(regexp = "INCOME|EXPENSE", message = "Type must be INCOME or EXPENSE")
    private String type;
}
