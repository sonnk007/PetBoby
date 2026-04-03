package com.sonnk.product.api;

import com.sonnk.sandbox.dto.ValidationErrorDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Global exception handler implementing RFC 7807: Problem Details for HTTP APIs.
 * Phase 2 Refactor: Migrated from custom ApiError to ProblemDetail.
 *
 * All error responses follow the standard ProblemDetail format with extensions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String GENERIC_ERROR_MESSAGE = "Internal server error";

    /**
     * Handle validation errors: @Valid failed, missing/invalid fields.
     * Returns 400 Bad Request with detailed validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("https://api.petboby.com/errors/validation-error"));
        pd.setTitle("Validation Failed");
        pd.setDetail("Input validation failed");
        pd.setInstance(URI.create(getRequestPath(request)));

        // Extract validation errors from binding result
        List<ValidationErrorDTO> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ValidationErrorDTO(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());

        // Add RFC 7807 extensions
        pd.setProperty("traceId", extractOrGenerateTraceId(request));
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("validationErrors", validationErrors);

        return ResponseEntity.badRequest().body(pd);
    }

    /**
     * Handle constraint violations: @NotNull, @Range, custom validators failed.
     * Returns 400 Bad Request with constraint violation details.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("https://api.petboby.com/errors/constraint-violation"));
        pd.setTitle("Constraint Violation");
        pd.setDetail("Business rule or constraint validation failed");
        pd.setInstance(URI.create(getRequestPath(request)));

        // Extract constraint violations
        List<ValidationErrorDTO> violations = ex.getConstraintViolations()
                .stream()
                .map(cv -> new ValidationErrorDTO(
                        cv.getPropertyPath().toString(),
                        cv.getMessage()
                ))
                .collect(Collectors.toList());

        pd.setProperty("traceId", extractOrGenerateTraceId(request));
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("validationErrors", violations);

        return ResponseEntity.badRequest().body(pd);
    }

    /**
     * Handle not found: EntityNotFoundException for missing resources.
     * Returns 404 Not Found.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setType(URI.create("https://api.petboby.com/errors/not-found"));
        pd.setTitle("Not Found");
        pd.setDetail(ex.getMessage() != null ? ex.getMessage() : "Resource not found");
        pd.setInstance(URI.create(getRequestPath(request)));

        pd.setProperty("traceId", extractOrGenerateTraceId(request));
        pd.setProperty("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    /**
     * Handle generic/unexpected exceptions.
     * Returns 500 Internal Server Error.
     *
     * Senior pattern: Log FULL details server-side (stack trace, variables, SQL).
     * Client receives GENERIC message only (no stack trace, no schema info).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception on {}", getRequestPath(request), ex);

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setType(URI.create("https://api.petboby.com/errors/internal-error"));
        pd.setTitle("Internal Server Error");
        pd.setDetail(GENERIC_ERROR_MESSAGE);
        pd.setInstance(URI.create(getRequestPath(request)));

        pd.setProperty("traceId", extractOrGenerateTraceId(request));
        pd.setProperty("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

    private String getRequestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        return query != null ? path + "?" + query : path;
    }

    private String extractOrGenerateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }
        return traceId;
    }
}

