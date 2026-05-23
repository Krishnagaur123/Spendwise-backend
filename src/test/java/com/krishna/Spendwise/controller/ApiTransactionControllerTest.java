package com.krishna.Spendwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishna.Spendwise.controller.api.ApiTransactionController;
import com.krishna.Spendwise.domain.dto.api.TransactionRequest;
import com.krishna.Spendwise.domain.dto.api.TransactionResponse;
import com.krishna.Spendwise.domain.dto.api.TransactionUpdateRequest;
import com.krishna.Spendwise.security.JwtRequestFilter;
import com.krishna.Spendwise.service.ApiTransactionService;
import com.krishna.Spendwise.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiTransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApiTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiTransactionService apiTransactionService;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void addTransaction_shouldReturnCreated() throws Exception {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("100"));
        request.setCategory("Salary");
        request.setDate(LocalDate.now());

        TransactionResponse response = TransactionResponse.builder()
            .id(1L)
            .amount(new BigDecimal("100"))
            .build();

        when(apiTransactionService.createTransaction(any())).thenReturn(response);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getAllTransactions_shouldReturnList() throws Exception {
        TransactionResponse response = TransactionResponse.builder().id(1L).build();
        
        when(apiTransactionService.getTransactions(null, null, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions[0].id").value(1L));
    }

    @Test
    void updateTransaction_shouldReturnOk() throws Exception {
        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setAmount(new BigDecimal("200"));

        TransactionResponse response = TransactionResponse.builder()
            .id(1L)
            .amount(new BigDecimal("200"))
            .build();

        when(apiTransactionService.updateTransaction(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(200));
    }

    @Test
    void deleteTransaction_shouldReturnNoContent() throws Exception {
        doNothing().when(apiTransactionService).deleteTransaction(1L);

        mockMvc.perform(delete("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction deleted successfully"));
    }
}
