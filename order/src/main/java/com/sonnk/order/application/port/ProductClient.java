package com.sonnk.order.application.port;

import com.sonnk.order.application.dto.ProductInfoDto;

import java.util.List;
import java.util.Optional;

/**
 * Port (interface) để Order service gọi Product service – Clean Architecture / DDD.
 * Application layer chỉ phụ thuộc interface; infrastructure implement bằng HTTP (RestTemplate/WebClient).
 *
 * Công dụng: tách dependency, dễ test (mock ProductClient), dễ đổi implementation (RestTemplate → WebClient).
 */
public interface ProductClient {

    /**
     * Lấy 1 sản phẩm theo id. Trả về Optional.empty() nếu product không tồn tại hoặc bị xóa.
     */
    Optional<ProductInfoDto> getProductById(Long id);

    /**
     * Lấy nhiều sản phẩm theo danh sách id (gọi GET /api/products/bulk?ids=...).
     * Tránh N+1: 1 request thay vì N request khi tạo đơn có nhiều dòng.
     */
    List<ProductInfoDto> getProductsByIds(List<Long> ids);
}
