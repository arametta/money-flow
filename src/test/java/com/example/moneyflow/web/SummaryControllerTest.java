package com.example.moneyflow.web;

import com.example.moneyflow.error.StatementNotFoundException;
import com.example.moneyflow.error.StatementTimeoutException;
import com.example.moneyflow.error.StatementUnavailableException;
import com.example.moneyflow.error.SummaryTimeoutException;
import com.example.moneyflow.error.SummaryUnavailableException;
import com.example.moneyflow.model.MonthlySummary;
import com.example.moneyflow.service.MoneyFlowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Checks that the controller maps requests and failures to the right HTTP status. */
@WebMvcTest(SummaryController.class)
class SummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MoneyFlowService moneyFlowService;

    @Test
    void returns200WithTheSummaryOnSuccess() throws Exception {
        MonthlySummary summary = new MonthlySummary(
                "acc-1",
                YearMonth.of(2026, 1),
                "EUR",
                new BigDecimal("100.00"),
                new BigDecimal("30.00"),
                new BigDecimal("70.00"),
                new BigDecimal("500.00"),
                new BigDecimal("570.00"));
        when(moneyFlowService.process(eq("acc-1"), eq(YearMonth.of(2026, 1)))).thenReturn(summary);

        mockMvc.perform(post("/summaries").param("accountId", "acc-1").param("month", "2026-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("acc-1"))
                .andExpect(jsonPath("$.closingBalance").value(570.00));
    }

    @Test
    void returns400WhenMonthIsMissing() throws Exception {
        mockMvc.perform(post("/summaries").param("accountId", "acc-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("month is required"));
    }

    @Test
    void returns400WhenMonthIsNotAValidFormat() throws Exception {
        mockMvc.perform(post("/summaries").param("accountId", "acc-1").param("month", "not-a-month"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("month must be in YYYY-MM format"));
    }

    @Test
    void returns400WhenAccountIdIsBlank() throws Exception {
        mockMvc.perform(post("/summaries").param("accountId", "").param("month", "2026-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("accountId must not be blank"));
    }

    @Test
    void returns404WhenStatementIsNotFound() throws Exception {
        when(moneyFlowService.process(any(), any())).thenThrow(
                new StatementNotFoundException("not found", null));

        mockMvc.perform(post("/summaries").param("accountId", "acc-1").param("month", "2026-01"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("not found"));
    }

    @Test
    void returns502WhenStatementsApiFails() throws Exception {
        when(moneyFlowService.process(any(), any())).thenThrow(
                new StatementUnavailableException("failed", null));

        mockMvc.perform(post("/summaries").param("accountId", "acc-1").param("month", "2026-01"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("failed"));
    }

    @Test
    void returns502WhenSummaryApiFails() throws Exception {
        when(moneyFlowService.process(any(), any())).thenThrow(
                new SummaryUnavailableException("failed", null));

        mockMvc.perform(post("/summaries").param("accountId", "acc-1").param("month", "2026-01"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("failed"));
    }

    @Test
    void returns504WhenStatementsApiTimesOut() throws Exception {
        when(moneyFlowService.process(any(), any())).thenThrow(
                new StatementTimeoutException("timed out", null));

        mockMvc.perform(post("/summaries").param("accountId", "acc-1").param("month", "2026-01"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.message").value("timed out"));
    }

    @Test
    void returns504WhenSummaryApiTimesOut() throws Exception {
        when(moneyFlowService.process(any(), any())).thenThrow(
                new SummaryTimeoutException("timed out", null));

        mockMvc.perform(post("/summaries").param("accountId", "acc-1").param("month", "2026-01"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.message").value("timed out"));
    }

    @Test
    void returns405ForUnsupportedMethod() throws Exception {
        mockMvc.perform(get("/summaries").param("accountId", "acc-1").param("month", "2026-01"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Method not allowed"));
    }

    @Test
    void returns404ForUnknownPath() throws Exception {
        mockMvc.perform(get("/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Not found"));
    }

    @Test
    void returns404ForFavicon() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Not found"));
    }

    @Test
    void returns500ForAnyUnexpectedError() throws Exception {
        when(moneyFlowService.process(any(), any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/summaries").param("accountId", "acc-1").param("month", "2026-01"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal error"));
    }
}
