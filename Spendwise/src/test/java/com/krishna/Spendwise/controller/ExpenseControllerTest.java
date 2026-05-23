package com.krishna.Spendwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishna.Spendwise.domain.dto.ExpenseDto;
import com.krishna.Spendwise.security.JwtRequestFilter;
import com.krishna.Spendwise.service.ExpenseService;
import com.krishna.Spendwise.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExpenseController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseService expenseService;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnExpenses() throws Exception {

        ExpenseDto dto = ExpenseDto.builder()
                .id(1L)
                .name("Lunch")
                .amount(new BigDecimal("200"))
                .date(LocalDate.now())
                .build();

        when(expenseService.getCurrentMonthExpensesForCurrentUser())
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Lunch"));
    }

    @Test
    void shouldAddExpense() throws Exception {

        ExpenseDto dto = ExpenseDto.builder()
                .name("Lunch")
                .amount(new BigDecimal("200"))
                .date(LocalDate.now())
                .build();

        when(expenseService.addExpense(any())).thenReturn(dto);

        mockMvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldDeleteExpense() throws Exception {

        mockMvc.perform(delete("/expenses/1"))
                .andExpect(status().isNoContent());
    }
}