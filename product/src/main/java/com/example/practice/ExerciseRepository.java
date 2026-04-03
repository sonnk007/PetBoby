package com.example.practice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * [PRACTICE] Repository minh hoạ kỹ thuật DTO Projection với Constructor Expression.
 *
 * ============================================================
 * PATTERN: Constructor Expression (JPQL "new" keyword)
 * ============================================================
 *
 * Cú pháp: SELECT new com.example.practice.ExerciseDto(e.id, e.title, e.price)
 *
 * Cơ chế hoạt động:
 *   1. Hibernate dịch thành SQL: SELECT id, title, price FROM practice_exercise
 *      (CHỈ 3 cột, KHÔNG phải SELECT *)
 *   2. JDBC ResultSet trả về 3 giá trị mỗi row.
 *   3. Hibernate gọi constructor ExerciseDto(Long, String, BigDecimal) cho mỗi row.
 *   4. Object ExerciseDto được tạo trực tiếp → KHÔNG đăng ký vào Persistence Context.
 *   5. Dirty checking KHÔNG chạy vì ExerciseDto không phải @Entity.
 *
 * So sánh với fetch full Entity:
 *   ┌─────────────────────────────┬──────────────────────┬─────────────────────────┐
 *   │                             │ Full Entity           │ DTO Projection          │
 *   ├─────────────────────────────┼──────────────────────┼─────────────────────────┤
 *   │ SQL columns fetched         │ SELECT * (6 cột)     │ SELECT id, title, price │
 *   │ Persistence Context         │ Managed (tracked)    │ Not managed             │
 *   │ Dirty checking snapshot     │ Có (tốn memory)      │ Không                   │
 *   │ Garbage Collection overhead │ Cao (full objects)   │ Thấp (3-field records)  │
 *   │ Use case phù hợp            │ Read + Write ops     │ Read-only display        │
 *   └─────────────────────────────┴──────────────────────┴─────────────────────────┘
 *
 * Khi nào NÊN dùng DTO Projection:
 *   - API trả về danh sách (list/paginated) chỉ để hiển thị.
 *   - Report/dashboard queries cần tổng hợp dữ liệu từ nhiều cột.
 *   - Bất kỳ query nào KHÔNG cần update entity sau khi đọc.
 *
 * Khi nào KHÔNG nên dùng (dùng full Entity):
 *   - Cần modify và save entity trong cùng transaction.
 *   - Cần cascade operations (persist children theo parent).
 *   - Logic domain phức tạp cần toàn bộ trạng thái entity.
 */
@Repository
public interface ExerciseRepository extends JpaRepository<ExerciseEntity, Long> {

    /**
     * Constructor Expression cơ bản: lấy tất cả, map thẳng vào DTO.
     *
     * SQL sinh ra:
     *   SELECT e.id, e.title, e.price FROM practice_exercise e
     *
     * Chú ý: tên đầy đủ của DTO (FQCN) phải được dùng trong JPQL.
     * Hibernate dùng reflection để gọi đúng constructor khớp với số và kiểu tham số.
     */
    @Query("select new com.example.practice.ExerciseDto(e.id, e.title, e.price) from ExerciseEntity e")
    List<ExerciseDto> findAllAsDto();

    /**
     * Constructor Expression với điều kiện lọc và phân trang.
     *
     * Kết hợp 3 kỹ thuật:
     *   1. DTO Projection → chỉ SELECT 3 cột cần thiết.
     *   2. WHERE clause   → lọc tại DB, không load toàn bộ bảng.
     *   3. Pageable       → LIMIT/OFFSET tại DB, kiểm soát kích thước result set.
     *
     * SQL sinh ra (ví dụ page=0, size=10, minPrice=50000):
     *   SELECT e.id, e.title, e.price
     *   FROM practice_exercise e
     *   WHERE e.price >= 50000
     *   LIMIT 10 OFFSET 0
     *
     * countQuery tách riêng để tránh SELECT new ... trong câu COUNT.
     */
    @Query(value = "select new com.example.practice.ExerciseDto(e.id, e.title, e.price) " +
                   "from ExerciseEntity e where e.price >= :minPrice",
           countQuery = "select count(e) from ExerciseEntity e where e.price >= :minPrice")
    Page<ExerciseDto> findByMinPriceAsDto(@Param("minPrice") BigDecimal minPrice, Pageable pageable);

    /**
     * Constructor Expression với ORDER BY — tìm top N bản ghi đắt nhất.
     *
     * Đây là pattern phổ biến cho "top products", "best sellers", v.v.
     * Vẫn chỉ SELECT 3 cột → tiết kiệm bandwidth dù lấy nhiều bản ghi.
     */
    @Query("select new com.example.practice.ExerciseDto(e.id, e.title, e.price) " +
           "from ExerciseEntity e order by e.price desc")
    List<ExerciseDto> findTopByPriceDescAsDto(Pageable pageable);
}
