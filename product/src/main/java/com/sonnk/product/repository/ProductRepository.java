package com.sonnk.product.repository;

import com.sonnk.product.model.entity.Product;
import com.sonnk.product.utils.enums.ProductStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // =========================================================================
    // [N+1 FIX] JOIN FETCH queries cho Product + Category (@ManyToOne)
    //
    // Vấn đề: Product.category là @ManyToOne(fetch = LAZY).
    //   Khi ProductController.toResponse() gọi product.getCategory().getId/getName(),
    //   JPA phát sinh 1 câu SELECT category riêng cho MỖI product trong list
    //   → N+1 queries (N = số sản phẩm trong trang).
    //
    // Giải pháp: left join fetch p.category trong cùng câu SELECT.
    //   SQL sinh ra: SELECT p.*, c.* FROM product p LEFT JOIN category c ON p.category_id = c.id
    //   → 1 query duy nhất lấy cả product lẫn category.
    //
    // Tại sao SAFE với Pageable?
    //   - Product.category là @ManyToOne (not a collection).
    //   - JOIN với bảng cha không nhân bản số hàng kết quả.
    //   - SQL LIMIT/OFFSET hoạt động chính xác → KHÔNG gây in-memory pagination.
    //   (Khác với @OneToMany collection fetch + Pageable → Hibernate cảnh báo HHH90003004)
    //
    // countQuery tách riêng để Spring tạo câu COUNT không có JOIN không cần thiết.
    // =========================================================================

    /**
     * [N+1 FIX] Lấy tất cả Product kèm Category trong 1 query, có phân trang.
     * Thay thế cho findAll(Pageable) khi cần truy cập product.getCategory().
     */
    @Query(value = "select p from Product p left join fetch p.category",
           countQuery = "select count(p) from Product p")
    List<Product> findAllWithCategory(Pageable pageable);

    /**
     * [N+1 FIX] Lọc theo status, JOIN FETCH category, có phân trang.
     * Thay thế cho findByStatus(status, pageable).
     */
    @Query(value = "select p from Product p left join fetch p.category where p.status = :status",
           countQuery = "select count(p) from Product p where p.status = :status")
    List<Product> findByStatusWithCategory(@Param("status") ProductStatus status, Pageable pageable);

    /**
     * [N+1 FIX] Lọc theo status + categoryId, JOIN FETCH category, có phân trang.
     * Dùng "join fetch" (inner) thay vì "left join fetch" vì categoryId đã được chỉ định rõ
     * → category không thể null, inner join hiệu quả hơn.
     * Thay thế cho findByStatusAndCategory_Id(status, categoryId, pageable).
     */
    @Query(value = "select p from Product p join fetch p.category c where p.status = :status and c.id = :categoryId",
           countQuery = "select count(p) from Product p where p.status = :status and p.category.id = :categoryId")
    List<Product> findByStatusAndCategoryIdWithCategory(@Param("status") ProductStatus status,
                                                         @Param("categoryId") Long categoryId,
                                                         Pageable pageable);
}
