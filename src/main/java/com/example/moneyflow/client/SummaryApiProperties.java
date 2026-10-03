package com.example.moneyflow.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "money-flow.summary-api")
public record SummaryApiProperties(String baseUrl, Duration timeout) {
}
