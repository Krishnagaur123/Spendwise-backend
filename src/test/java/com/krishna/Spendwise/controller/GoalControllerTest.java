package com.krishna.Spendwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishna.Spendwise.controller.api.GoalController;
import com.krishna.Spendwise.domain.dto.api.GoalRequest;
import com.krishna.Spendwise.domain.dto.api.GoalResponse;
import com.krishna.Spendwise.domain.dto.api.GoalUpdateRequest;
import com.krishna.Spendwise.security.JwtRequestFilter;
import com.krishna.Spendwise.service.GoalService;
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

@WebMvcTest(GoalController.class)
@AutoConfigureMockMvc(addFilters = false)
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoalService goalService;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void addGoal_shouldReturnCreated() throws Exception {
        GoalRequest request = new GoalRequest();
        request.setGoalName("Car");
        request.setTargetAmount(new BigDecimal("5000"));
        request.setTargetDate(LocalDate.now().plusMonths(5));

        GoalResponse response = GoalResponse.builder()
            .id(1L)
            .goalName("Car")
            .build();

        when(goalService.createGoal(any())).thenReturn(response);

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getAllGoals_shouldReturnList() throws Exception {
        GoalResponse response = GoalResponse.builder()
            .id(1L)
            .build();
        
        when(goalService.getAllGoals()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goals[0].id").value(1L));
    }

    @Test
    void updateGoal_shouldReturnOk() throws Exception {
        GoalUpdateRequest request = new GoalUpdateRequest();
        request.setTargetAmount(new BigDecimal("6000"));
        request.setTargetDate(LocalDate.now().plusMonths(3));

        GoalResponse response = GoalResponse.builder()
            .id(1L)
            .goalName("Car")
            .targetAmount(new BigDecimal("6000"))
            .build();

        when(goalService.updateGoal(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/goals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetAmount").value(6000));
    }

    @Test
    void deleteGoal_shouldReturnNoContent() throws Exception {
        doNothing().when(goalService).deleteGoal(1L);

        mockMvc.perform(delete("/api/goals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Goal deleted successfully"));
    }
}
