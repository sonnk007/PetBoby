package com.sonnk.product.demo;

import com.sonnk.product.model.entity.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller DEMO – chỉ phục vụ học tập (scope, JPA state, AOP).
 * Không thuộc luồng nghiệp vụ chính; có thể tắt hoặc bỏ qua khi deploy prod.
 */
@RestController
@RequestMapping("/api/demo")
public class ProductDemoController {

    private final ProductDemoService demoService;

    public ProductDemoController(ProductDemoService demoService) {
        this.demoService = demoService;
    }

    /**
     * Demo IoC + bean scope: singleton / prototype / request.
     */
    @GetMapping("/scopes")
    public ResponseEntity<Map<String, String>> scopes() {
        return ResponseEntity.ok(demoService.demoScopes());
    }

    /**
     * Demo JPA persistence context + dirty checking.
     * - Nếu không truyền id: sẽ tạo product mới.
     * - Nếu truyền id: load product, đổi name mà không gọi save().
     */
    @PostMapping("/jpa")
    public ResponseEntity<Product> createDemoProduct() {
        Product p = demoService.demoJpaPersistenceContext(null);
        return ResponseEntity.ok(p);
    }

    @PostMapping("/jpa/{id}")
    public ResponseEntity<Product> updateDemoProduct(@PathVariable("id") Long id) {
        Product p = demoService.demoJpaPersistenceContext(id);
        return ResponseEntity.ok(p);
    }
}

