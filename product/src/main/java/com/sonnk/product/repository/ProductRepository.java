package com.sonnk.product.repository;

import com.sonnk.product.model.entity.Product;
import com.sonnk.product.utils.enums.ProductStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository truy cập Product (JPA).
 *
 * Derived queries (theo tên method) giúp:
 * - Lọc dữ liệu ngay tại DB, giảm tải memory và network.
 * - Spring Data JPA sinh implementation tự động, không cần viết JPQL/SQL tay.
 * Hạn chế: tên method dài, query phức tạp nên dùng @Query.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * find… → SELECT; By → điều kiện WHERE; Status → theo property status.
     * Trả về danh sách product có status cho trước, có phân trang (Pageable).
     */
    List<Product> findByStatus(ProductStatus status, Pageable pageable);

    /**
     * By … And … → nối nhiều điều kiện WHERE; Category_Id → điều kiện theo khóa ngoại category.
     * Lọc product theo status và category, có phân trang. Dùng cho API menu theo nhóm (vd: ACTIVE + categoryId=1).
     */
    List<Product> findByStatusAndCategory_Id(ProductStatus status, Long categoryId, Pageable pageable);

    /**
     * HasToppingTrue → WHERE has_topping = true; And Status → thêm điều kiện status.
     * Lấy sản phẩm có hỗ trợ topping và đúng trạng thái (vd: list món có topping đang bán).
     */
    List<Product> findByHasToppingTrueAndStatus(ProductStatus status, Pageable pageable);

    /**
     * IdIn → WHERE id IN (…): lấy nhiều product theo danh sách id truyền vào.
     * Dùng để load bulk danh sách product trong 1 query thay vì gọi findById nhiều lần (tránh N+1 ở phía service).
     */
    List<Product> findByIdIn(List<Long> ids);
}
