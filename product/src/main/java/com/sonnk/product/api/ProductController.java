package com.sonnk.product.api;

import com.sonnk.product.api.dto.ProductCreateRequest;
import com.sonnk.product.api.dto.ProductResponse;
import com.sonnk.product.api.dto.ProductUpdateRequest;
import com.sonnk.product.model.dtos.ToppingInfo;
import com.sonnk.product.model.entity.Category;
import com.sonnk.product.model.entity.Product;
import com.sonnk.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * RESTful API thực chiến cho module product (menu đồ uống).
 *
 * Nguyên tắc:
 * - Dùng DTO (request/response) thay vì trả entity thô.
 * - Trả về HTTP status code đúng ngữ nghĩa (200, 201, 204, 404...).
 * - Để GlobalExceptionHandler xử lý lỗi & chuẩn hoá error response.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll() {
        List<ProductResponse> responses = productService.getAllProducts().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        Product product = productService.getProductOrThrow(id);
        return ResponseEntity.ok(toResponse(product));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        Product saved = productService.create(request);
        return ResponseEntity
                .created(URI.create("/api/products/" + saved.getId()))
                .body(toResponse(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ProductUpdateRequest request) {
        Product saved = productService.update(id, request);
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestParam(defaultValue = "system") String deletedBy) {
        productService.softDelete(id, deletedBy);
    }

    /**
     * API RESTful demo cho yêu cầu roadmap:
     * - Lấy danh sách topping gắn với 1 sản phẩm cụ thể.
     */
    @GetMapping("/{id}/toppings")
    public ResponseEntity<List<ToppingInfo>> getToppings(@PathVariable Long id) {
        Product product = productService.getProductOrThrow(id);
        return ResponseEntity.ok(product.getToppings());
    }

    private ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getProductSize(),
                product.getHasTopping(),
                product.getStatus(),
                category != null ? category.getId() : null,
                category != null ? category.getCode() : null,
                category != null ? category.getName() : null,
                product.getImageUrl()
        );
    }
}

