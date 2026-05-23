package com.krishna.Spendwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishna.Spendwise.domain.dto.CategoryDto;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

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
    void shouldReturnCategories() throws Exception {

        CategoryDto dto = CategoryDto.builder()
                .id(1L)
                .name("Food")
                .type("EXPENSE")
                .build();

        when(categoryService.getCategoriesForCurrentUser())
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Food"));
    }

    @Test
    void shouldCreateCategory() throws Exception {

        CategoryDto dto = CategoryDto.builder()
                .name("Food")
                .type("EXPENSE")
                .build();

        when(categoryService.saveCategory(any())).thenReturn(dto);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }
}