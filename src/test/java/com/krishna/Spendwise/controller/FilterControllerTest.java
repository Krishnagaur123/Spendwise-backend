package com.krishna.Spendwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishna.Spendwise.domain.dto.FilterDto;
import com.krishna.Spendwise.domain.dto.ExpenseDto;
import com.krishna.Spendwise.domain.dto.IncomeDto;
import com.krishna.Spendwise.security.JwtRequestFilter;
import com.krishna.Spendwise.service.ExpenseService;
import com.krishna.Spendwise.service.IncomeService;
import com.krishna.Spendwise.util.JwtUtil;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;

@WebMvcTest(FilterController.class)
@AutoConfigureMockMvc(addFilters = false)
class FilterControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseService expenseService;

    @MockitoBean
    private IncomeService incomeService;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldFilterExpenses() throws Exception {

        FilterDto filter = new FilterDto();
        filter.setType("expense");

        ExpenseDto dto = ExpenseDto.builder()
                .name("Lunch")
                .build();

        when(expenseService.filterExpenses(any(), any(), any(), any()))
                .thenReturn(List.of(dto));

        mockMvc.perform(post("/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Lunch"));
    }

    @Test
    void shouldFilterIncomes() throws Exception {

        FilterDto filter = new FilterDto();
        filter.setType("income");

        IncomeDto dto = IncomeDto.builder()
                .name("Salary")
                .build();

        when(incomeService.filterIncomes(any(), any(), any(), any()))
                .thenReturn(List.of(dto));

        mockMvc.perform(post("/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Salary"));
    }

    @Test
    void shouldReturnBadRequestForInvalidType() throws Exception {

        FilterDto filter = new FilterDto();
        filter.setType("invalid");

        mockMvc.perform(post("/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isBadRequest());
    }

}
