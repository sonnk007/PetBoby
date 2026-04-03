# Summary Change Log - DB Optimization Analysis

## Quickstart Analysis
Current project status:
- `Product` module: Has many N+1 fixes but `ProductController.getById` still triggers a lazy load for `Category` (1+1).
- `Order` module: Uses `batch_fetch_size=50` to mitigate N+1, but could be further optimized for performance.
- SQL Logging: Mostly enabled but needs enhanced query display.

## Identification of N+1 Queries
1. **Case 1: Product Detail Fetch**
   - **File:** `ProductController.java` calling `productService.getProductOrThrow(id)`.
   - **Reason:** `ProductRepository.findById(id)` fetches only the product. Mapping to `ProductResponse` calls `product.getCategory()`, triggering a second query.
   - **Solution:** Use `JOIN FETCH` for single entity fetch.

2. **Case 2: Order List Fetch**
   - **File:** `OrderController.java` calling `orderService.getAllOrdersPaged(pageable)`.
   - **Reason:** Even with `batch_fetch_size`, it results in 3+ queries (Orders, Items, Toppings).
   - **Solution:** Implement DTO Projection to fetch flat data for the list view or use `@EntityGraph` if pagination can be handled safely.

## Deliverables Created
- `product/src/main/java/com/sonnk/sandbox/db/ProductN1FixRepository.java`: Demo JOIN FETCH.
- `order/src/main/java/com/sonnk/sandbox/db/OrderN1FixRepository.java`: Demo @EntityGraph.
- `product/src/main/java/com/sonnk/sandbox/db/ProductListDto.java`: Record for projection.
- `product/src/main/java/com/sonnk/sandbox/db/ProductProjectionRepository.java`: Demo DTO Projection + Pagination.

## Phase 3 Analysis
1. **JOIN FETCH vs Standard findById:**
   - `findById`: 1 SQL for Product + 1 SQL for Category (Lazy).
   - `findByIdWithCategory`: 1 SQL with LEFT JOIN.
2. **EntityGraph on Collections:**
   - `findAllWithItems`: Fetches orders and items in a single query (or as directed by Hibernate). Efficient but risks Cartesian product if multiple collections are fetched.
3. **DTO Projections:**
   - `findAllProjected`: Only selects `id, name, price, categoryName`. Dramatically reduces memory footprint and avoids persistence context management for the DTOs.

## Phase 5: Verification & Metrics (Sandbox Results)

### 1. N+1 Verification (Product Detail)
- **Old (findById):**
    - Query 1: `SELECT * FROM product WHERE id = ?`
    - Query 2: `SELECT * FROM category WHERE id = ?` (Triggered during mapping)
    - **Total: 2 queries per request.**
- **New (findByIdWithCategory):**
    - Query 1: `SELECT p.*, c.* FROM product p LEFT JOIN category c ON p.category_id = c.id WHERE p.id = ?`
    - **Total: 1 query per request (Reduction: 50%).**

### 2. DTO Projection Verification (Product List)
- **Standard Entity Fetch:**
    - SQL: `SELECT * FROM product ...` (Fetches all columns: code, description, image_url, etc.)
    - Memory: Managed by Persistence Context (Dirty checking overhead).
- **DTO Projection (ProductListDto):**
    - SQL: `SELECT p.id, p.name, p.price, c.name FROM product p ...`
    - **Optimization:** Only 4 columns selected. No dirty checking overhead. Significant memory reduction for large lists.

### 3. Collection Fetching (Orders)
- **Batch Fetch (Current Production):** 1 + N/50 queries.
- **EntityGraph (Sandbox):** 1 query (using JOIN).
- **Warning:** EntityGraph with `@OneToMany` + `Pageable` will trigger HHH90003004 (in-memory pagination) if not handled carefully with a separate count query or by limiting fetch size.

## Final Conclusion
The sandbox implementations successfully demonstrate the reduction of query counts and memory footprint. Skipping production apply as per user request. Project is ready for production migration whenever needed.


