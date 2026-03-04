package com.sonnk.product.repository;

import com.sonnk.product.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository truy cập Category (JPA).
 * Dùng các method kế thừa từ JpaRepository:
 * - findById(Long id): tìm một category theo PK; trả về Optional; thường dùng kèm orElseThrow ở service.
 * - findAll(): trả về toàn bộ (cẩn thận khi data lớn); có overload nhận Pageable/Sort.
 * - save(entity): persist hoặc merge entity.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
