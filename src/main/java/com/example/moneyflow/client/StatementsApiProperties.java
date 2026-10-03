package com.example.moneyflow.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Settings for connecting to the statements API. */
@ConfigurationProperties(prefix = "money-flow.statements-api")
public record StatementsApiProperties(String baseUrl, Duration timeout) {
}
