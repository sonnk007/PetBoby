# Implementation Plan: Trace‑ID Based Request Logging

## 1. Trace‑ID Generation
- Create a `TraceIdFilter` extending `OncePerRequestFilter` in each microservice (`auth`, `gateway-service`, `order`, `product`).
- On each request, check for header `X-Trace-Id`. If missing, generate a UUID (`UUID.randomUUID().toString()`).
- Store the trace‑id as a request attribute, add it to the response header, and put it into **MDC** (`MDC.put("traceId", traceId)`).
- Clear MDC in a `finally` block.

## 2. MDC Propagation
- All log statements can now include `%X{traceId}` in the pattern.
- Ensure any async tasks copy the MDC context (e.g., using `MdcTaskDecorator`).

## 3. Database Schema for Log Entries
```sql
CREATE TABLE request_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id VARCHAR(36) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    http_method VARCHAR(10),
    uri VARCHAR(255),
    status INT,
    request_body TEXT,
    response_body TEXT,
    duration_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
- Add a JPA entity `RequestLog` in a new `logging` package for each service.

## 4. Persistence Service
- Interface `RequestLogRepository` extends `JpaRepository<RequestLog, Long>`.
- Service `RequestLogService` with method `saveLog(RequestLog log)`.
- Autowire the service in `TraceIdFilter` and persist the log after the request completes.

## 5. Optional AOP Logging Aspect
- Define annotation `@Loggable`.
- Implement `LoggingAspect` that surrounds methods annotated with `@Loggable`, logs entry/exit, parameters, execution time, and uses the MDC trace‑id.

## 6. Logback Configuration for ELK
Update each service's `logback‑spring.xml`:
```xml
<appender name="ELK" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <destination>elk-host:5000</destination>
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
        <providers>
            <timestamp/>
            <pattern>
                <pattern>{"traceId":"%X{traceId}","service":"${spring.application.name}"}</pattern>
            </pattern>
            <logLevel/>
            <loggerName/>
            <threadName/>
            <message/>
            <stackTrace/>
        </providers>
    </encoder>
</appender>
<root level="INFO">
    <appender-ref ref="ELK"/>
</root>
```
- Ensure the pattern includes `%X{traceId}`.

## 7. Modifications to Existing Filters
- In `JwtAuthenticationFilter` (auth) and `GatewayRateLimiterFilter` (gateway), add `MDC.put("traceId", traceId)` at the start and clear it at the end.
- When making outbound HTTP calls (RestTemplate/Feign), add an interceptor that copies the MDC value to the `X-Trace-Id` header.

## 8. Maven Dependencies
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```
- Ensure `spring-boot-starter-aop` is present for the aspect.

## 9. Unit‑Test Strategy
- Test `TraceIdFilter` generates/propagates the id and sets MDC.
- Mock `RequestLogRepository` to verify `saveLog` is called with correct data.
- Aspect tests using a dummy service annotated with `@Loggable`.
- Integration test for the RestTemplate interceptor confirming header propagation.

## 10. Migration Steps
1. Add Flyway migration script to create `request_log` table.
2. Deploy the new filter and logging changes to each service.
3. Verify logs in ELK contain the `traceId` and can be queried across services.
4. Enable DB persistence behind a feature flag, monitor performance, then fully enable.

## 11. Documentation
- Add a **Trace‑ID Logging** section to each service README describing the header, MDC usage, and how to query logs in ELK.
- Update `Implementation_Plan_JWT_Auth.md` with a link to this file.

---
**Next Steps**: Review the plan, approve, and proceed with implementation.

