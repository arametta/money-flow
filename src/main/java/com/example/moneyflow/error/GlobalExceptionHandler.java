package com.example.moneyflow.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.YearMonth;

/** Turns known failures into simple error responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException e) {
        return badRequest(e, e.getParameterName() + " is required");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String rule = YearMonth.class.equals(e.getRequiredType()) ? "must be in YYYY-MM format" : "has an invalid value";
        return badRequest(e, e.getName() + " " + rule);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleValidation(HandlerMethodValidationException e) {
        ParameterValidationResult result = e.getParameterValidationResults().get(0);
        String name = result.getMethodParameter().getParameterName();
        return badRequest(e, name + " " + result.getResolvableErrors().get(0).getDefaultMessage());
    }

    @ExceptionHandler(StatementNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(StatementNotFoundException e) {
        return errorResponse(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler({StatementTimeoutException.class, SummaryTimeoutException.class})
    public ResponseEntity<ApiError> handleTimeout(RuntimeException e) {
        return errorResponse(HttpStatus.GATEWAY_TIMEOUT, e);
    }

    @ExceptionHandler({StatementUnavailableException.class, SummaryUnavailableException.class})
    public ResponseEntity<ApiError> handleBadGateway(RuntimeException e) {
        return errorResponse(HttpStatus.BAD_GATEWAY, e);
    }

    // Spring's own exceptions already carry the right status; only unknown exceptions become 500.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e) {
        if (e instanceof ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            String reason = status.getReasonPhrase();
            return errorResponse(status, e, reason.charAt(0) + reason.substring(1).toLowerCase());
        }
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError("Internal error"));
    }

    private ResponseEntity<ApiError> errorResponse(HttpStatus status, Exception e) {
        return errorResponse(status, e, e.getMessage());
    }

    private ResponseEntity<ApiError> badRequest(Exception e, String message) {
        return errorResponse(HttpStatus.BAD_REQUEST, e, message);
    }

    private ResponseEntity<ApiError> errorResponse(HttpStatus status, Exception e, String message) {
        if (status.is5xxServerError()) {
            log.warn("Request failed with {}: {}", status, message, e);
        } else {
            log.warn("Request failed with {}: {}", status, message);
        }
        return ResponseEntity.status(status).body(new ApiError(message));
    }
}
