package com.krishna.Spendwise.domain.dto.api;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApiCategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;

    @NotBlank(message = "Category type is required")
    private String type; // "INCOME" or "EXPENSE"
}
