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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        return badRequest(e, e.getParameterName() + " is required");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return badRequest(e, "month must be in YYYY-MM format");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        return badRequest(e, "accountId must not be blank");
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
        return errorResponse(status, e, e.getMessage());
    }

    private ResponseEntity<ErrorResponse> badRequest(Exception e, String message) {
        return errorResponse(HttpStatus.BAD_REQUEST, e, message);
    }

    private ResponseEntity<ErrorResponse> errorResponse(HttpStatus status, Exception e, String message) {
        log.warn("Request failed with {}: {}", status, message, e);
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}
