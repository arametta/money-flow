package com.example.moneyflow.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "money-flow.statements-api")
public record StatementsApiProperties(String baseUrl, Duration timeout) {
}
