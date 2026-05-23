package com.krishna.Spendwise.controller;

import com.krishna.Spendwise.controller.api.ReportController;
import com.krishna.Spendwise.domain.dto.api.MonthlyReportResponse;
import com.krishna.Spendwise.domain.dto.api.YearlyReportResponse;
import com.krishna.Spendwise.security.JwtRequestFilter;
import com.krishna.Spendwise.service.ReportService;
import com.krishna.Spendwise.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void getMonthlyReport_shouldReturnData() throws Exception {
        MonthlyReportResponse res = MonthlyReportResponse.builder().month(5).build();
        when(reportService.getMonthlyReport(2023, 5)).thenReturn(res);

        mockMvc.perform(get("/api/reports/monthly/2023/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(5));
    }

    @Test
    void getYearlyReport_shouldReturnData() throws Exception {
        YearlyReportResponse res = YearlyReportResponse.builder().year(2023).build();
        when(reportService.getYearlyReport(2023)).thenReturn(res);

        mockMvc.perform(get("/api/reports/yearly/2023"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2023));
    }
}
