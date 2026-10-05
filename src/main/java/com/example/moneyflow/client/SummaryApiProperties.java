package com.example.moneyflow.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Settings for connecting to the summary API. */
@Validated
@ConfigurationProperties(prefix = "money-flow.summary-api")
public record SummaryApiProperties(@NotBlank String baseUrl, @NotNull Duration timeout) {
}
