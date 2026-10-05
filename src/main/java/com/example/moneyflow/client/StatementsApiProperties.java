package com.example.moneyflow.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Settings for connecting to the statements API. */
@Validated
@ConfigurationProperties(prefix = "money-flow.statements-api")
public record StatementsApiProperties(@NotBlank String baseUrl, @NotNull Duration timeout) {
}
