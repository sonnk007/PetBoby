package com.sonnk.order.application.exception;

/**
 * Ném khi productId trong request không tồn tại hoặc không active ở Product service.
 * Map sang 400 Bad Request trong GlobalExceptionHandler.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String message) {
        super(message);
    }
}
