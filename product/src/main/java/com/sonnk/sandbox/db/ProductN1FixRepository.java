package com.sonnk.sandbox.db;

import com.sonnk.product.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductN1FixRepository extends JpaRepository<Product, Long> {

    /**
     * [N+1 IDENTIFIED]
     * Standard findById triggers a second query for Category when mapping to DTO.
     */
    @Override
    Optional<Product> findById(Long id);

    /**
     * [N+1 FIXED]
     * JOIN FETCH ensures Category is loaded in the same query.
     */
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);
}
