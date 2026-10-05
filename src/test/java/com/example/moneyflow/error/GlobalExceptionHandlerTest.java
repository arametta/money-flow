package com.example.moneyflow.error;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks that errors are logged at the right level, with a stack trace only for server errors. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        appender = new ListAppender<>();
        appender.start();
        logger().addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger().detachAppender(appender);
    }

    private Logger logger() {
        return (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    }

    @Test
    void logsClientErrorsWithoutStackTrace() {
        handler.handleNotFound(new StatementNotFoundException("not found", null));

        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getThrowableProxy()).isNull();
    }

    @Test
    void logsUnknownPathWithoutStackTrace() {
        handler.handleUnexpected(new NoResourceFoundException(HttpMethod.GET, "/nope", "nope"));

        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getThrowableProxy()).isNull();
    }

    @Test
    void logsServerErrorsWithStackTrace() {
        handler.handleBadGateway(new StatementUnavailableException("failed", new RuntimeException("cause")));

        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getThrowableProxy()).isNotNull();
    }

    @Test
    void logsUnexpectedErrorsWithStackTrace() {
        handler.handleUnexpected(new IllegalStateException("boom"));

        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getThrowableProxy()).isNotNull();
    }
}
