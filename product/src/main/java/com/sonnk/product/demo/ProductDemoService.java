package com.sonnk.product.demo;

import com.sonnk.product.model.entity.Product;
import com.sonnk.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.sonnk.product.demo.ScopedBeans.PrototypeBean;
import static com.sonnk.product.demo.ScopedBeans.RequestBean;
import static com.sonnk.product.demo.ScopedBeans.SingletonBean;

@Service
public class ProductDemoService {

    private static final Logger log = LoggerFactory.getLogger(ProductDemoService.class);

    private final SingletonBean singletonBean;
    private final PrototypeBean prototypeBean;
    private final RequestBean requestBean;
    private final ProductRepository productRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ProductDemoService(SingletonBean singletonBean,
                              PrototypeBean prototypeBean,
                              RequestBean requestBean,
                              ProductRepository productRepository) {
        this.singletonBean = singletonBean;
        this.prototypeBean = prototypeBean;
        this.requestBean = requestBean;
        this.productRepository = productRepository;
    }

    @DemoLogged
    public Map<String, String> demoScopes() {
        Map<String, String> result = new HashMap<>();
        result.put("singleton", singletonBean.info());
        result.put("prototype_first", prototypeBean.info());
        // gọi lại prototype thông qua EntityManager để thấy instance khác nhau nếu lấy mới
        PrototypeBean anotherPrototype = entityManager.getEntityManagerFactory()
                .getPersistenceUnitUtil() != null ? prototypeBean : prototypeBean;
        result.put("prototype_second", anotherPrototype.info());
        result.put("request", requestBean.info());
        return result;
    }

    @DemoLogged
    @Transactional
    public Product demoJpaPersistenceContext(Long productId) {
        Product p;
        if (productId == null) {
            p = new Product();
            p.setName("DEMO-" + UUID.randomUUID());
            p.setDescription("Created in demoJpaPersistenceContext");
            p.setPrice(BigDecimal.TEN);
            log.info("[DEMO-JPA] New transient product name={}", p.getName());
            productRepository.save(p);
            log.info("[DEMO-JPA] After save, id={}", p.getId());
        } else {
            Optional<Product> opt = productRepository.findById(productId);
            if (opt.isEmpty()) {
                throw new IllegalArgumentException("Product not found for id=" + productId);
            }
            p = opt.get();
            log.info("[DEMO-JPA] Loaded managed product id={}, name={}", p.getId(), p.getName());
            String newName = p.getName() + "-UPDATED";
            p.setName(newName);
            log.info("[DEMO-JPA] Changed name in memory to {}", newName);
            // Không gọi save() lại; dirty checking + flush tại commit sẽ update DB.
        }
        return p;
    }

    /**
     * Demo merge/clear trong JPA.
     * - KHÔNG được dùng trong luồng nghiệp vụ chính, chỉ để quan sát state Managed/Detached/Merged.
     * - Không được expose qua bất kỳ controller nào trong hệ thống.
     *
     * Gợi ý sử dụng khi học:
     *   - Gọi từ một test hoặc từ main method riêng để xem log và SQL sinh ra.
     */
    @Transactional
    public void demoMergeAndClear(Long productId) {
        if (productId == null) {
            return;
        }
        Product managed = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found for id=" + productId));
        log.info("[DEMO-JPA] Managed entity before clear, id={}, name={}", managed.getId(), managed.getName());

        // Xoá persistence context → entity trở thành DETACHED
        entityManager.clear();

        managed.setName(managed.getName() + "-DETACHED");
        log.info("[DEMO-JPA] Detached entity changed in memory, sẽ KHÔNG được flush vì đã clear PC");

        // Tạo bản copy managed mới qua merge
        Product merged = entityManager.merge(managed);
        log.info("[DEMO-JPA] Merged entity, id={}, name={}", merged.getId(), merged.getName());
        // Khi commit transaction, JPA sẽ flush UPDATE dựa trên state của merged.
    }

    /**
     * Demo self-invocation:
     * - internalLoggedMethod() có @DemoLogged.
     * - Khi gọi thông qua selfInvocationEntry() (call nội bộ cùng class),
     *   call này KHÔNG đi qua proxy AOP nên advice trong DemoLoggingAspect sẽ KHÔNG chạy.
     *
     * Ghi chú:
     * - Không expose selfInvocationEntry() qua bất kỳ controller nào.
     * - Chỉ sử dụng từ test hoặc main riêng khi bạn muốn quan sát behavior self-invocation.
     */
    public void selfInvocationEntry() {
        internalLoggedMethod();
    }

    @DemoLogged
    void internalLoggedMethod() {
        log.info("[DEMO-AOP] internalLoggedMethod đang chạy (KHÔNG qua proxy nếu gọi nội bộ).");
    }
}

