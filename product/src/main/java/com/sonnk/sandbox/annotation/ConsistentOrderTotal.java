package com.sonnk.sandbox.annotation;

import com.sonnk.sandbox.validator.ConsistentOrderTotalValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom constraint validator for order totals.
 * Ensures finalAmount = totalAmount - totalDiscount.
 *
 * Usage: Apply to OrderCreateRequest record
 * @ConsistentOrderTotal
 * public record OrderCreateRequest(
 *     @NotNull BigDecimal totalAmount,
 *     @NotNull BigDecimal totalDiscount,
 *     @NotNull BigDecimal finalAmount
 * ) {}
 *
 * If finalAmount doesn't equal (totalAmount - totalDiscount),
 * validation fails with detailed error message.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConsistentOrderTotalValidator.class)
@Documented
public @interface ConsistentOrderTotal {
    String message() default "finalAmount must equal (totalAmount - totalDiscount)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
