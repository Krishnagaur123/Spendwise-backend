package com.krishna.Spendwise.controller.api;

import com.krishna.Spendwise.domain.dto.api.ApiCategoryRequest;
import com.krishna.Spendwise.domain.dto.api.ApiCategoryResponse;
import com.krishna.Spendwise.domain.dto.api.MessageResponse;
import com.krishna.Spendwise.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class ApiCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<Map<String, List<ApiCategoryResponse>>> getCategories() {
        return ResponseEntity.ok(Map.of("categories", categoryService.getCategoriesWithDefaults()));
    }

    @PostMapping
    public ResponseEntity<ApiCategoryResponse> createCategory(@Valid @RequestBody ApiCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCustomCategory(request));
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<MessageResponse> deleteCategory(@PathVariable String name) {
        categoryService.deleteByName(name);
        return ResponseEntity.ok(new MessageResponse("Category deleted successfully"));
    }
}
