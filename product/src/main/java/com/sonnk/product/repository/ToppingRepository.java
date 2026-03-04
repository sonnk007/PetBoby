package com.sonnk.product.repository;

import com.sonnk.product.model.entity.Topping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository truy cập Topping (JPA).
 * Dùng các method kế thừa từ JpaRepository:
 * - findById(Long id): tìm một topping theo PK; trả về Optional.
 * - findAll(): trả về toàn bộ topping; service product dùng để list topping cho menu.
 * - save(entity): persist hoặc merge.
 */
@Repository
public interface ToppingRepository extends JpaRepository<Topping, Long> {
}
