package com.krishna.Spendwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishna.Spendwise.controller.api.ApiCategoryController;
import com.krishna.Spendwise.domain.dto.api.ApiCategoryRequest;
import com.krishna.Spendwise.domain.dto.api.ApiCategoryResponse;
import com.krishna.Spendwise.security.JwtRequestFilter;
import com.krishna.Spendwise.service.CategoryService;
import com.krishna.Spendwise.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApiCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllCategories_shouldReturnList() throws Exception {
        ApiCategoryResponse response = ApiCategoryResponse.builder().name("Food").build();
        
        when(categoryService.getCategoriesWithDefaults()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].name").value("Food"));
    }

    @Test
    void addCategory_shouldReturnCreated() throws Exception {
        ApiCategoryRequest request = new ApiCategoryRequest();
        request.setName("Salary");
        request.setType("INCOME");

        ApiCategoryResponse response = ApiCategoryResponse.builder().name("Salary").build();

        when(categoryService.createCustomCategory(any())).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Salary"));
    }

    @Test
    void deleteCategory_shouldReturnOk() throws Exception {
        doNothing().when(categoryService).deleteByName("Salary");

        mockMvc.perform(delete("/api/categories/Salary"))
                .andExpect(status().isOk());
    }
}
