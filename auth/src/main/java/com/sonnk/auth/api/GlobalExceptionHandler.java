package com.sonnk.auth.api;

import com.sonnk.auth.api.dto.ValidationErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Global exception handler cho auth service.
 * Phase 2 Refactor: Migrated from custom ApiError to ProblemDetail RFC 7807.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String GENERIC_ERROR_MESSAGE = "Internal server error";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setType(URI.create("https://api.petboby.com/errors/unauthorized"));
        pd.setTitle("Unauthorized");
        pd.setDetail(ex.getMessage());
        pd.setInstance(URI.create(getRequestPath(request)));

        pd.setProperty("traceId", extractOrGenerateTraceId(request));
        pd.setProperty("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex,
                                                        HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("https://api.petboby.com/errors/validation-error"));
        pd.setTitle("Validation Failed");
        pd.setDetail("Input validation failed");
        pd.setInstance(URI.create(getRequestPath(request)));

        List<ValidationErrorDTO> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ValidationErrorDTO(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());

        pd.setProperty("traceId", extractOrGenerateTraceId(request));
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("validationErrors", validationErrors);

        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("https://api.petboby.com/errors/constraint-violation"));
        pd.setTitle("Constraint Violation");
        pd.setDetail("Business rule or constraint validation failed");
        pd.setInstance(URI.create(getRequestPath(request)));

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest request) {
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

