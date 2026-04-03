package com.sonnk.sandbox.db;

import com.sonnk.product.model.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductProjectionRepository extends JpaRepository<Product, Long> {

    /**
     * [EXERCISE: DTO PROJECTION]
     * Only select 4 fields instead of all entity fields.
     * Efficient for large lists.
     */
    @Query("""
        SELECT new com.sonnk.sandbox.db.ProductListDto(
            p.id, p.name, p.price, c.name
        )
        FROM Product p
        LEFT JOIN p.category c
    """)
    Page<ProductListDto> findAllProjected(Pageable pageable);
}
