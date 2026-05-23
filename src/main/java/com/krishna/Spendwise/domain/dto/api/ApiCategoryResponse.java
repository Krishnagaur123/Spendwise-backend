package com.krishna.Spendwise.domain.dto.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiCategoryResponse {
    private String name;
    private String type;
    private boolean isCustom;
}
