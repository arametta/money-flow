package com.example.moneyflow.error;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Turns known failures into simple error responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return errorResponse(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler(StatementNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(StatementNotFoundException e) {
        return errorResponse(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(StatementTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(StatementTimeoutException e) {
        return errorResponse(HttpStatus.GATEWAY_TIMEOUT, e);
    }

    @ExceptionHandler({StatementUnavailableException.class, SummaryUnavailableException.class})
    public ResponseEntity<ErrorResponse> handleBadGateway(RuntimeException e) {
        return errorResponse(HttpStatus.BAD_GATEWAY, e);
    }

    private ResponseEntity<ErrorResponse> errorResponse(HttpStatus status, Exception e) {
        log.warn("Request failed with {}: {}", status, e.getMessage(), e);
        return ResponseEntity.status(status).body(new ErrorResponse(e.getMessage()));
    }
}
