package com.example.moneyflow.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ApiPropertiesValidationTest {

    @EnableConfigurationProperties({StatementsApiProperties.class, SummaryApiProperties.class})
    static class Config {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class)
            .withPropertyValues(
                    "money-flow.statements-api.base-url=http://localhost:8081",
                    "money-flow.statements-api.timeout=5s",
                    "money-flow.summary-api.base-url=http://localhost:8081",
                    "money-flow.summary-api.timeout=5s");

    @Test
    void startsWithValidConfig() {
        runner.run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void failsOnBlankStatementsBaseUrl() {
        runner.withPropertyValues("money-flow.statements-api.base-url=")
                .run(context -> assertFailsOn(context.getStartupFailure(), "money-flow.statements-api", "baseUrl"));
    }

    @Test
    void failsOnMissingStatementsTimeout() {
        runner.withPropertyValues("money-flow.statements-api.timeout=")
                .run(context -> assertFailsOn(context.getStartupFailure(), "money-flow.statements-api", "timeout"));
    }

    @Test
    void failsOnBlankSummaryBaseUrl() {
        runner.withPropertyValues("money-flow.summary-api.base-url=")
                .run(context -> assertFailsOn(context.getStartupFailure(), "money-flow.summary-api", "baseUrl"));
    }

    @Test
    void failsOnMissingSummaryTimeout() {
        runner.withPropertyValues("money-flow.summary-api.timeout=")
                .run(context -> assertFailsOn(context.getStartupFailure(), "money-flow.summary-api", "timeout"));
    }

    private static void assertFailsOn(Throwable failure, String prefix, String field) {
        assertThat(failure).isNotNull();
        assertThat(failure).rootCause()
                .isInstanceOf(BindValidationException.class)
                .hasMessageContaining(prefix)
                .hasMessageContaining("'" + field + "'");
    }
}
