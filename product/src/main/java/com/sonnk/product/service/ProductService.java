package com.sonnk.product.service;

import com.sonnk.product.config.ProductAppProperties;
import com.sonnk.product.api.dto.ProductCreateRequest;
import com.sonnk.product.api.dto.ProductUpdateRequest;
import com.sonnk.product.model.entity.Category;
import com.sonnk.product.model.entity.Product;
import com.sonnk.product.model.entity.Topping;
import com.sonnk.product.repository.CategoryRepository;
import com.sonnk.product.repository.ProductRepository;
import com.sonnk.product.repository.ToppingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service nghiệp vụ chính cho Product.
 * - Dùng constructor injection (IoC + DI "chuẩn").
 * - Dùng @Transactional để quản lý persistence context + dirty checking.
 * - Không chứa logic demo, chạy trong luồng thật của hệ thống.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ToppingRepository toppingRepository;
    private final ProductAppProperties properties;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          ToppingRepository toppingRepository,
                          ProductAppProperties properties) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.toppingRepository = toppingRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        // Demo Spring Boot fundamentals: dùng cấu hình type-safe từ ProductAppProperties.
        // defaultPageSize có thể cấu hình khác nhau giữa môi trường (dev/stage/prod).
        int pageSize = properties.getDefaultPageSize();
        return productRepository.findAll(PageRequest.of(0, pageSize)).getContent();
    }

    @Transactional(readOnly = true)
    public Product getProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found, id=" + id));
    }

    @Transactional
    public Product create(ProductCreateRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found, id=" + request.categoryId()));

        Product product = new Product();
        product.setCode(request.code());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setProductSize(request.productSize());
        product.setHasTopping(request.hasTopping());
        product.setStatus(request.status());
        product.setCategory(category);
        product.setImageUrl(request.imageUrl());

        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, ProductUpdateRequest request) {
        Product product = getProductOrThrow(id);

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found, id=" + request.categoryId()));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setProductSize(request.productSize());
        product.setHasTopping(request.hasTopping());
        product.setStatus(request.status());
        product.setCategory(category);
        product.setImageUrl(request.imageUrl());

        return productRepository.save(product);
    }

    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public void softDelete(Long id, String deletedBy) {
        Product product = getProductOrThrow(id);
        product.markDeleted(deletedBy);
    }

    @Transactional
    public Product updateProductBasicInfo(Long id, String name, String description) {
        Product p = getProductOrThrow(id);
        p.setName(name);
        p.setDescription(description);
        // Không gọi save(): để JPA dirty checking + transaction tự flush.
        return p;
    }

    @Transactional(readOnly = true)
    public List<Topping> getAllToppings() {
        return toppingRepository.findAll();
    }
}

