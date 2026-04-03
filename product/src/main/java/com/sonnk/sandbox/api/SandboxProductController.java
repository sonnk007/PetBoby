package com.sonnk.sandbox.api;

import com.sonnk.sandbox.dto.ProductDetailProjection;
import com.sonnk.sandbox.dto.ProductProjection;
import com.sonnk.sandbox.repository.SandboxProductRepository;
import com.sonnk.product.utils.enums.ProductStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Sandbox controller demonstrating DTO projection patterns.
 * All methods return lightweight DTOs (projections) instead of full entities.
 *
 * HTTP Status Codes:
 * - 200 OK: GET, PUT operations
 * - 201 Created: POST operations (with Location header)
 * - 204 No Content: DELETE operations (no response body)
 * - 400 Bad Request: Validation errors (ProblemDetail)
 * - 404 Not Found: Resource not found (ProblemDetail)
 *
 * Note: This sandbox is non-production and focuses on query optimization.
 * Actual POST/PUT operations would require service layer integration.
 */
@RestController
@RequestMapping("/api/sandbox/products")
public class SandboxProductController {

    private final SandboxProductRepository productRepository;

    public SandboxProductController(SandboxProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * GET /api/sandbox/products?status=ACTIVE
     * Fetch products by status using projection.
     *
     * Response: 200 OK
     * Body: List<ProductProjection> (id, name, price only)
     *
     * Benefits:
     * - SQL: SELECT p.id, p.name, p.price FROM product p WHERE p.status = 'ACTIVE'
     * - No category lazy-loading (no N+1)
     * - Reduced network payload (~85% smaller than full entity)
     * - No dirty-tracking overhead
     */
    @GetMapping
    public ResponseEntity<List<ProductProjection>> getByStatus(
            @RequestParam ProductStatus status) {
        List<ProductProjection> projections = productRepository.findByStatus(status);
        return ResponseEntity.ok(projections);
    }

    /**
     * GET /api/sandbox/products/{id}
     * Fetch single product with category using detailed projection.
     *
     * Response: 200 OK
     * Body: ProductDetailProjection (id, code, name, price, categoryCode, categoryName)
     *
     * Benefits:
     * - SQL: Single LEFT JOIN query, avoids N+1
     * - Includes category details without lazy-load
     * - Reduced payload vs full entity + nested category entity
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailProjection> getById(@PathVariable Long id) {
        Optional<ProductDetailProjection> projection = productRepository.findByIdWithCategory(id);

        if (projection.isEmpty()) {
            throw new jakarta.persistence.EntityNotFoundException(
                    "Product not found: " + id);
        }

        return ResponseEntity.ok(projection.get());
    }

    /**
     * POST /api/sandbox/products
     * Create a new product.
     *
     * Response: 201 Created
     * Header: Location: /api/sandbox/products/{id}
     * Body: ProductProjection
     *
     * Note: For sandbox demo.
     * Real implementation would require DTO→Entity mapping and service layer.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Void> create() {
        // Sandbox: simplified for demo
        Long newId = 999L;
        return ResponseEntity
                .created(URI.create("/api/sandbox/products/" + newId))
                .build();
    }

    /**
     * DELETE /api/sandbox/products/{id}
     * Soft-delete a product.
     *
     * Response: 204 No Content
     * Body: (empty)
     *
     * Note: For sandbox demo.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        // Sandbox: simplified for demo
        // Real implementation: productService.softDelete(id, "system");
    }
}
