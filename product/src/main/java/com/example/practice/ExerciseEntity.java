package com.example.practice;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [PRACTICE] Entity minh hoạ kỹ thuật DTO Projection.
 *
 * Entity này có 6 trường nhưng phần lớn tác vụ "chỉ đọc để hiển thị"
 * chỉ cần 2-3 trường → fetch toàn bộ Entity là lãng phí:
 *
 *   1. Persistence Context phải quản lý snapshot của toàn bộ entity
 *      (tất cả 6 trường) để hỗ trợ dirty checking.
 *   2. Với 10.000 bản ghi, 6 trường × 10.000 rows → bộ nhớ heap lớn
 *      dù chỉ cần hiển thị tên và giá.
 *   3. Garbage Collector phải thu dọn tất cả các object entity sau request.
 *
 * Giải pháp: DTO Projection → chỉ SELECT đúng 2-3 cột cần thiết,
 *   kết quả map thẳng vào Java Record → KHÔNG nằm trong Persistence Context
 *   → không có dirty checking → tiết kiệm CPU + heap.
 */
@Entity
@Table(name = "practice_exercise")
@Getter
@Setter
@NoArgsConstructor
public class ExerciseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tên khoá học / sản phẩm luyện tập */
    @Column(nullable = false, length = 200)
    private String title;

    /** Tác giả / người tạo */
    @Column(nullable = false, length = 100)
    private String author;

    /** Giá (BigDecimal để tránh sai số floating-point) */
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    /** Mô tả dài — thường không cần khi chỉ hiển thị danh sách */
    @Column(length = 2000)
    private String description;

    /** Thời điểm tạo */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Số lượng lượt xem — chỉ cần cho analytics, không cần cho display list */
    private Long viewCount;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
