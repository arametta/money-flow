package com.example.moneyflow.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Settings for connecting to the summary API. */
@ConfigurationProperties(prefix = "money-flow.summary-api")
public record SummaryApiProperties(String baseUrl, Duration timeout) {
}
