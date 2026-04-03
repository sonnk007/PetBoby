package com.sonnk.sandbox.validator;

import com.sonnk.sandbox.annotation.ConsistentOrderTotal;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

/**
 * Validator for @ConsistentOrderTotal constraint.
 * Checks that finalAmount = totalAmount - totalDiscount.
 *
 * This is a class-level validator (applies to entire object, not field).
 * Called during bean validation, before controller business logic.
 *
 * Example scenario:
 * Order submitted with:
 * - totalAmount: 100.00
 * - totalDiscount: 30.00
 * - finalAmount: 65.00  ← WRONG (should be 70.00)
 *
 * Result: Validation fails, 400 Bad Request returned with message.
 * Business logic never reaches ServiceLayer.
 */
public class ConsistentOrderTotalValidator
        implements ConstraintValidator<ConsistentOrderTotal, Object> {

    @Override
    public void initialize(ConsistentOrderTotal annotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // If not applicable (e.g., null), let other @NotNull handle it
        if (value == null) {
            return true;
        }

        // Reflection: get totalAmount, totalDiscount, finalAmount fields
        try {
            // Assuming the validated object has these fields as public/accessible via reflection
            // For a record: may use record component accessors
            Object totalAmountObj = getFieldValue(value, "totalAmount");
            Object totalDiscountObj = getFieldValue(value, "totalDiscount");
            Object finalAmountObj = getFieldValue(value, "finalAmount");

            if (totalAmountObj == null || totalDiscountObj == null || finalAmountObj == null) {
                return true; // Let @NotNull validate
            }

            // Assume BigDecimal (typical for monetary values)
            BigDecimal totalAmount = (BigDecimal) totalAmountObj;
            BigDecimal totalDiscount = (BigDecimal) totalDiscountObj;
            BigDecimal finalAmount = (BigDecimal) finalAmountObj;

            BigDecimal expected = totalAmount.subtract(totalDiscount);
            boolean isValid = finalAmount.compareTo(expected) == 0;

            if (!isValid) {
                // Customize error message with actual values
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        String.format(
                                "finalAmount (%.2f) must equal totalAmount (%.2f) - totalDiscount (%.2f) = %.2f",
                                finalAmount, totalAmount, totalDiscount, expected
                        )
                ).addConstraintViolation();
            }

            return isValid;
        } catch (Exception e) {
            // If reflection fails, don't fail validation (other validators handle it)
            return true;
        }
    }

    /**
     * Utility: get field value from object using reflection.
     * Works with records, POJOs, and classes with getters.
     */
    private Object getFieldValue(Object obj, String fieldName) throws Exception {
        try {
            // Try direct field access (works for records and public fields)
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException e) {
            // Try getter method (e.g., getTotalAmount() for totalAmount)
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            try {
                var method = obj.getClass().getMethod(getterName);
                return method.invoke(obj);
            } catch (NoSuchMethodException ex) {
                // If record component accessor
                try {
                    var method = obj.getClass().getMethod(fieldName);
                    return method.invoke(obj);
                } catch (NoSuchMethodException ignore) {
                    return null;
                }
            }
        }
    }
}
