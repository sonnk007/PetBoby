# PetBoby Backend Standards

This project is built with a **Domain-Driven Design (DDD) + Clean Architecture** mindset, optimized for high scalability (5M concurrent users target).

## Engineering Standards

### 1. Architecture & Layering
Follow the strict layering per service:
- **api/web**: Controllers, Request/Response DTOs, Mapping.
- **application/service**: Use cases, Transaction boundaries (`@Transactional`), coordination.
- **domain**: Core logic, Entities, Value Objects, Domain Services. **No dependencies on other layers.**
- **infrastructure**: JPA Repositories, external clients (Kafka, Redis, HTTP), technical configs.

### 2. Spring Boot 3 & Java 17+
- Use `jakarta.*` namespace for JPA/Validation (avoid `javax.*`).
- **Constructor Injection** only. No `@Autowired` on fields.
- Update deprecated APIs immediately (e.g., use `authorizeHttpRequests()` in Security 6).
- **Global Exception Handling**: Use `@RestControllerAdvice` and a unified `ApiError` format.

### 3. Database & Performance (Enterprise Scale)
- **No N+1 Queries**: Use `join fetch`, `@EntityGraph`, or `findByIdIn`.
- **Mandatory Pagination**: All list APIs must use `Pageable`.
- **Read/Write Separation**: Use `@Transactional(readOnly = true)` for queries.
- **Indexing**: Ensure all columns used in WHERE/JOIN/ORDER BY are indexed.

### 4. Code Generation & Documentation
- **JPA Repositories**: Every method (derived or `@Query`) MUST have a JavaDoc explaining its query logic (e.g., `findByIdIn` -> "Finds all entities by ID collection; avoids N+1").
- **Design Choices**: After generating code, explain the applied pattern (DDD, Clean Arch, etc.) and its trade-offs.
- **Demo Index**: Update `docs/PetBoby-backend-demo-index.md` whenever adding new demo code or knowledge.

## Critical Mandates
- **Validation**: Use `jakarta.validation` on DTOs.
- **Immutability**: Prefer Value Objects and immutable DTOs.
- **Security**: Never log sensitive data. Use environment variables for secrets.
- **Idempotency**: External consumers (Kafka) must have idempotency checks.

Refer to `docs/PetBoby-backend-coding-rules.md` for the full detailed specification.
