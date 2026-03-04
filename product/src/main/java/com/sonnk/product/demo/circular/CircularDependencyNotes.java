package com.sonnk.product.demo.circular;

/**
 * Ví dụ CIRCULAR DEPENDENCY chỉ dùng để học, KHÔNG dùng trong runtime.
 *
 * - Hai class AService và BService bên dưới được viết ra để minh hoạ
 *   circular dependency qua constructor injection sẽ khiến Spring không resolve được.
 * - TẤT CẢ annotation @Component, @Service đều được comment lại
 *   để tránh bị Spring scan và gây lỗi vòng đời bean.
 *
 * Nếu muốn tự thử nghiệm:
 *   1. Bật lại @Component cho AService và BService.
 *   2. Chạy ứng dụng và quan sát lỗi khởi động (circular reference).
 *
 * Quan trọng: code này chỉ là "tài liệu sống" cho kiến thức IoC + 3-level cache,
 * không thuộc luồng nghiệp vụ chính của hệ thống.
 */
public class CircularDependencyNotes {

//    @Component
    static class AService {
        private final BService bService;

//        @Autowired
        public AService(BService bService) {
            this.bService = bService;
        }
    }

//    @Component
    static class BService {
        private final AService aService;

//        @Autowired
        public BService(AService aService) {
            this.aService = aService;
        }
    }
}

