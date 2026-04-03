package com.example.practice;

import java.math.BigDecimal;

/**
 * [PRACTICE] DTO Projection dưới dạng Java Record.
 *
 * Chỉ chứa 3 trường thực sự cần thiết để hiển thị danh sách:
 *   - id       : định danh để build link chi tiết
 *   - title    : hiển thị tên
 *   - price    : hiển thị giá
 *
 * Các trường KHÔNG có ở đây (author, description, createdAt, viewCount):
 *   - Không được SELECT từ DB → tốn ít bandwidth hơn.
 *   - Không được ánh xạ thành Java object → tốn ít heap hơn.
 *   - Không nằm trong Persistence Context → không có dirty checking snapshot.
 *
 * Tại sao dùng Java Record (thay vì class thông thường)?
 *   - Record là IMMUTABLE (tất cả trường final) → thread-safe mặc định.
 *   - Compiler tự sinh equals(), hashCode(), toString() → ít boilerplate.
 *   - Truyền tải rõ ý định: "đây là data container chỉ đọc, không phải domain entity".
 *
 * Dirty Checking KHÔNG áp dụng cho DTO:
 *   - Entity ở trạng thái MANAGED → Hibernate theo dõi mọi thay đổi.
 *   - DTO Record này không có @Entity, không nằm trong Persistence Context
 *     → Hibernate không biết đến nó → không snapshot → không dirty check
 *     → tiết kiệm CPU đáng kể khi xử lý hàng nghìn bản ghi.
 */
public record ExerciseDto(
        Long id,
        String title,
        BigDecimal price
) {
}
