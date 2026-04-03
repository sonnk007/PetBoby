package com.sonnk.sandbox.repository;

import com.sonnk.sandbox.dto.CategoryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.sonnk.product.model.entity.Category;

import java.util.List;

/**
 * Sandbox category repository with projection example.
 * Demonstrates simple projection without joins.
 */
@Repository
public interface SandboxCategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Fetch all categories as projections (lightweight).
     * Use case: Dropdown list of categories (GET /categories)
     *
     * SQL generated:
     * SELECT c.id, c.code, c.name FROM category c WHERE c.deleted_at IS NULL
     */
    @Query("SELECT new com.sonnk.sandbox.dto.CategoryProjection(" +
           "c.id, c.code, c.name) " +
           "FROM Category c")
    List<CategoryProjection> findAllCategories();
}
