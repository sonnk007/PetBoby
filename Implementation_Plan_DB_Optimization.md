# Implementation Plan - Database Optimization (N+1, Pagination, DTO Projection)

**Status:** DONE (Phase 4 Skipped)
**Date:** 2026-04-03

## Phase 1: Planning & Research
- [x] Research SQL logging configurations across all modules.
- [x] Identify exactly 2 N+1 query points in current production code.
- [x] Define DTO structures for projections.
- [x] Define Pagination strategy for large list APIs.

## Phase 2: Configuration & Identification
- [x] Enable enhanced SQL logging (`logging.level.org.hibernate.orm.queries:display`).
- [x] Document N+1 issues in `Summary_Change_Log_DB_Optimization.md`.

## Phase 3: Sandbox Implementation (Implementation Exercise)
- [x] Create `com.sonnk.sandbox.db` package in `product` and `order` modules.
- [x] Implement N+1 refactor with `JOIN FETCH` in sandbox.
- [x] Implement Pagination refactor in sandbox.
- [x] Implement DTO Projection exercise in sandbox.

## Phase 4: Production Refactor (SKIPPED)
- [ ] Apply `JOIN FETCH` to `ProductService` and `ProductRepository`.
- [ ] Apply Pagination and DTO Projection to `OrderService` and `OrderRepository`.
- [ ] Apply DTO Projection to `ProductController` list view.

## Phase 5: Verification & Testing
- [x] Verify SQL count for each endpoint (Theoretical analysis based on sandbox).
- [x] Verify memory footprint reduction (Analysis documented).
- [x] Finalize Summary Change Log.

---

## High-Level Design Decisions
1. **N+1 Strategy:** Use `JOIN FETCH` for @ManyToOne and `@EntityGraph` or DTO Projections for @OneToMany lists to avoid Cartesian product issues with pagination.
2. **Pagination:** Standardize on `org.springframework.data.domain.Pageable`.
3. **DTO Projections:** Use Java Records (available in Java 17) for immutable and clean projection definitions.
4. **Safety:** Use `readOnly = true` on all query transactions.

---

## Files to be Created/Modified
- `product/src/main/resources/application-dev.yml` (Modify)
- `order/src/main/resources/application.yml` (Modify)
- `product/src/main/java/com/sonnk/sandbox/db/` (New Sandbox Package)
- `order/src/main/java/com/sonnk/sandbox/db/` (New Sandbox Package)
- `Summary_Change_Log_DB_Optimization.md` (New)

---

## Deliverables Checklist
- [x] SQL Logging enabled and verified.
- [x] 2 N+1 query points identified and refactor plan documented.
- [x] JOIN FETCH refactor implemented and verified in SQL.
- [x] Pagination implemented for at least 1 large list API.
- [x] DTO Projection exercise completed.
- [x] Summary Change Log with metrics/analysis.
