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
import com.sonnk.product.utils.enums.ProductStatus;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Application service cho use case Product (DDD light + Clean Architecture).
 *
 * Pattern áp dụng:
 * - Service layer: điều phối repository + domain entity, đặt transaction boundary (@Transactional).
 * - Constructor injection: dễ test, không field injection.
 *
 * Công dụng: một chỗ thực thi use case (create/update/softDelete/getByStatus…), controller chỉ gọi service.
 * Hạn chế: khi nghiệp vụ phức tạp hơn có thể tách domain service (vd: tính giá theo topping) hoặc CQRS.
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

    /**
     * Danh sách sản phẩm theo trạng thái và (optional) category.
     * Dùng derived query tại DB → chỉ lấy đúng dữ liệu cần, có phân trang.
     */
    @Transactional(readOnly = true)
    public List<Product> getByStatusAndCategory(ProductStatus status, Long categoryId) {
        int pageSize = properties.getDefaultPageSize();
        PageRequest page = PageRequest.of(0, pageSize);
        if (categoryId == null) {
            return productRepository.findByStatus(status, page);
        }
        return productRepository.findByStatusAndCategory_Id(status, categoryId, page);
    }

    /**
     * Lấy nhiều product theo danh sách id, dùng query IN thay vì gọi findById trong vòng lặp.
     * Senior: một số DB giới hạn số tham số IN (vd Oracle ~1000); list lớn chia batch để tránh lỗi.
     */
    private static final int BULK_SELECT_BATCH_SIZE = 500;

    @Transactional(readOnly = true)
    public List<Product> getByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        if (ids.size() <= BULK_SELECT_BATCH_SIZE) {
            return productRepository.findByIdIn(ids);
        }
        List<Product> result = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += BULK_SELECT_BATCH_SIZE) {
            int to = Math.min(i + BULK_SELECT_BATCH_SIZE, ids.size());
            result.addAll(productRepository.findByIdIn(ids.subList(i, to)));
        }
        return result;
    }
}

