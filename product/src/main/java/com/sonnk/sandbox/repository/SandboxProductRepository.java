package com.sonnk.sandbox.repository;

import com.sonnk.product.model.entity.Product;
import com.sonnk.product.utils.enums.ProductStatus;
import com.sonnk.sandbox.dto.ProductDetailProjection;
import com.sonnk.sandbox.dto.ProductProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Sandbox repository demonstrating:
 * 1. Constructor expressions for DTO projections
 * 2. JOIN FETCH for single-root entity loads
 * 3. Query optimization to reduce N+1 issues
 *
 * Pattern: Use constructor expressions to instantiate DTO records directly in JPQL.
 * This reduces columns fetched, prevents dirty-checking overhead, and improves memory usage.
 */
@Repository
public interface SandboxProductRepository extends JpaRepository<Product, Long> {

    /**
     * Simple projection: fetch only id, name, price.
     * Use case: Product list view (GET /products?status=ACTIVE)
     *
     * SQL generated:
     * SELECT p.id, p.name, p.price FROM product p WHERE p.status = 'ACTIVE' AND p.deleted_at IS NULL
     *
     * Benefits vs full entity fetch:
     * - Reduces columns: 20+ → 3
     * - No dirty-checking: record is immutable
     * - Network payload: ~85% smaller
     * - Memory: 3 primitives vs full object graph
     */
    @Query("SELECT new com.sonnk.sandbox.dto.ProductProjection(" +
           "p.id, p.name, p.price) " +
           "FROM Product p " +
           "WHERE p.status = :status")
    List<ProductProjection> findByStatus(@Param("status") ProductStatus status);

    /**
     * Detailed projection: includes category information.
     * Use case: Product detail view with category info (GET /products/{id})
     *
     * Note: LEFT JOIN (not FETCH) because this is a projection, not a persistent object.
     * FETCH joins are only for entity hydration, not projections.
     *
     * SQL generated:
     * SELECT p.id, p.code, p.name, p.price, c.code, c.name
     * FROM product p
     * LEFT JOIN category c ON p.category_id = c.id
     * WHERE p.id = :id AND p.deleted_at IS NULL
     *
     * Avoids N+1: Single query brings category data without triggering lazy-load.
     */
    @Query("SELECT new com.sonnk.sandbox.dto.ProductDetailProjection(" +
           "p.id, p.code, p.name, p.price, c.code, c.name) " +
           "FROM Product p " +
           "LEFT JOIN p.category c " +
           "WHERE p.id = :id")
    Optional<ProductDetailProjection> findByIdWithCategory(@Param("id") Long id);

    /**
     * Fetch full entity WITH category (single-root, non-paginated).
     * Use case: Update/delete operations that need all fields for validation or cascading.
     *
     * FETCH join allowed here because:
     * - Single-root (product, not categories) - no Cartesian product
     * - Non-paginated - no pagination issues
     * - Result is persistent entity (can update/delete)
     *
     * SQL generated:
     * SELECT DISTINCT p.*, c.* FROM product p
     * LEFT JOIN FETCH p.category c
     * WHERE p.id = :id AND p.deleted_at IS NULL
     *
     * Avoids N+1: Single query brings category in same result set.
     */
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.id = :id")
    Optional<Product> findByIdWithCategoryForUpdate(@Param("id") Long id);

    /**
     * Find multiple products by status using IN clause with projection.
     * Use case: Bulk fetch with filtering (GET /products/bulk?status=ACTIVE,PENDING)
     *
     * SQL generated:
     * SELECT p.id, p.name, p.price FROM product p
     * WHERE p.status IN ('ACTIVE', 'PENDING') AND p.deleted_at IS NULL
     */
    @Query("SELECT new com.sonnk.sandbox.dto.ProductProjection(" +
           "p.id, p.name, p.price) " +
           "FROM Product p " +
           "WHERE p.status IN :statuses")
    List<ProductProjection> findByStatusIn(@Param("statuses") List<ProductStatus> statuses);
}
