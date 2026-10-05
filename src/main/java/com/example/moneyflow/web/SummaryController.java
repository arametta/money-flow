package com.example.moneyflow.web;

import com.example.moneyflow.model.MonthlySummary;
import com.example.moneyflow.service.MoneyFlowService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

/** Receives a request to build and send one monthly summary. */
@RestController
public class SummaryController {

    private final MoneyFlowService moneyFlowService;

    public SummaryController(MoneyFlowService moneyFlowService) {
        this.moneyFlowService = moneyFlowService;
    }

    @PostMapping("/summaries")
    public MonthlySummary createSummary(
            @RequestParam
            @NotBlank(message = "must not be blank")
            @Size(max = 64, message = "must be at most 64 characters")
            @Pattern(regexp = "[A-Za-z0-9-]*", message = "must contain only letters, digits and '-'")
            String accountId,
            @RequestParam YearMonth month) {
        return moneyFlowService.process(accountId, month);
    }
}
