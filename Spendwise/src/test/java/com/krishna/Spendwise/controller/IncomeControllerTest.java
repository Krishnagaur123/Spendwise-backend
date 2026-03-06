package com.krishna.Spendwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishna.Spendwise.domain.dto.IncomeDto;
import com.krishna.Spendwise.security.JwtRequestFilter;
import com.krishna.Spendwise.service.IncomeService;
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

@WebMvcTest(IncomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class IncomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncomeService incomeService;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnIncomes() throws Exception {

        IncomeDto dto = IncomeDto.builder()
                .id(1L)
                .name("Salary")
                .amount(new BigDecimal("50000"))
                .date(LocalDate.now())
                .build();

        when(incomeService.getCurrentMonthIncomesForCurrentUser())
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/incomes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Salary"));
    }

    @Test
    void shouldAddIncome() throws Exception {

        IncomeDto dto = IncomeDto.builder()
                .name("Salary")
                .amount(new BigDecimal("50000"))
                .date(LocalDate.now())
                .build();

        when(incomeService.addIncome(any())).thenReturn(dto);

        mockMvc.perform(post("/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldDeleteIncome() throws Exception {

        mockMvc.perform(delete("/incomes/1"))
                .andExpect(status().isNoContent());
    }
}