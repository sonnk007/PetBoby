package com.sonnk.sandbox.annotation;

import com.sonnk.sandbox.validator.AllowedProductStateValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom constraint validator for product state transitions.
 * Ensures product state follows valid transitions: PENDING → ACTIVE → INACTIVE
 * Only specific transitions are allowed (no arbitrary state changes).
 *
 * Allowed transitions:
 * - PENDING → ACTIVE
 * - ACTIVE → INACTIVE
 * - INACTIVE ← no outgoing transitions (terminal state)
 *
 * Usage: Apply to ProductUpdateRequest.status field
 * public record ProductUpdateRequest(
 *     @AllowedProductState String newStatus
 * ) {}
 *
 * If transition is not allowed, validation fails.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AllowedProductStateValidator.class)
@Documented
public @interface AllowedProductState {
    String message() default "Invalid product state transition";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
