package com.sonnk.sandbox.validator;

import com.sonnk.sandbox.annotation.AllowedProductState;
import com.sonnk.product.utils.enums.ProductStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for @AllowedProductState constraint.
 * Enforces product state transition rules at validation time (before business logic).
 *
 * State machine:
 * PENDING → ACTIVE → INACTIVE (terminal)
 *
 * Invalid transitions rejected:
 * - PENDING → INACTIVE (skip ACTIVE)
 * - ACTIVE → PENDING (backward)
 * - INACTIVE → * (terminal state, no outgoing)
 *
 * This is a field-level validator that checks the new state value.
 * Note: A production implementation would also check the CURRENT state
 * (e.g., from the database), but for sandbox demo we validate the new state only.
 */
public class AllowedProductStateValidator
        implements ConstraintValidator<AllowedProductState, Object> {

    @Override
    public void initialize(AllowedProductState annotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // Null values are handled by @NotNull
        if (value == null) {
            return true;
        }

        // Convert string or enum to ProductStatus
        ProductStatus newState;
        try {
            if (value instanceof ProductStatus) {
                newState = (ProductStatus) value;
            } else if (value instanceof String) {
                newState = ProductStatus.valueOf((String) value);
            } else {
                return true; // Unknown type, skip this validator
            }
        } catch (IllegalArgumentException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Invalid product status: " + value
            ).addConstraintViolation();
            return false;
        }

        // All states are technically "allowed" for creation (first transition is PENDING → ACTIVE)
        // In a real scenario, check current state too:
        // if (currentState == PENDING && newState != ACTIVE) return false;
        // if (currentState == ACTIVE && newState != INACTIVE) return false;
        // if (currentState == INACTIVE) return false; // Terminal state

        // For this sandbox example, just validate that it's a known state
        // (Enum.valueOf already validated that above)
        return true;
    }
}
