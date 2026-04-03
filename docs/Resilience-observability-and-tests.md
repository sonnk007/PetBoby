# Resilience Observability and Test Plan

This document describes minimal observability and test steps for the Resilience features implemented in PetBoby.

## Metrics (gateway)
- Gateway exposes actuator metrics and Prometheus endpoint at `/actuator/prometheus`.
- Important metrics provided by the gateway rate limiter filter:
  - `gateway_rate_limiter_allowed_total` – total requests allowed by rate limiter
  - `gateway_rate_limiter_rejected_total` – total requests rejected (HTTP 429)

## How to enable and access metrics
1. Build and run `gateway-service` with actuator and micrometer present.
2. Request metrics:

```bash
curl http://localhost:8080/actuator/prometheus
```

Look for the two counters above.

## Manual test plan for gateway rate limiter
1. Start `gateway-service` (port 8080) and a downstream service (e.g., `product` on 8082).
2. Send a burst of requests through the gateway for a single client IP:

```bash
for i in {1..20}; do curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/products/1 & done
wait
```

Expected: first 10 requests succeed (200), subsequent requests return 429.

3. Check metrics after the test:

```bash
curl http://localhost:8080/actuator/prometheus | grep gateway_rate_limiter
```

## Unit / Integration test suggestions
- Unit test for `GatewayRateLimiterFilter`: simulate exchanges with different remote addresses and assert allowed vs rejected counters and status code.
- Integration test for `ProductClient`: mock Product service (WireMock) to return failures then success, assert retries and fallback.

## Notes
- Current gateway implementation uses per-instance in-memory token-bucket. For production, replace with distributed rate limiter backed by Redis or use a cloud API Gateway feature.
- Ensure `management.endpoints.web.exposure.include=prometheus,health,metrics` in application properties for Prometheus scraping.
