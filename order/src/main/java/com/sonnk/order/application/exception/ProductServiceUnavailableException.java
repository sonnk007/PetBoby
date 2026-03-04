package com.sonnk.order.application.exception;

/**
 * Ném khi không gọi được Product service (timeout, 5xx, network).
 * Dùng trong ProductRestClient; GlobalExceptionHandler map sang 503 hoặc 502.
 */
public class ProductServiceUnavailableException extends RuntimeException {

    public ProductServiceUnavailableException(String message) {
        super(message);
    }

    public ProductServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
