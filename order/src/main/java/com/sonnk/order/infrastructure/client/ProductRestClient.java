package com.sonnk.order.infrastructure.client;

import com.sonnk.order.application.dto.ProductInfoDto;
import com.sonnk.order.application.exception.ProductServiceUnavailableException;
import com.sonnk.order.application.port.ProductClient;
import com.sonnk.order.config.ProductServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Implementation của ProductClient bằng RestTemplate (Tuần 3 – Synchronous communication).
 *
 * Công dụng:
 * - Order service gọi Product service qua HTTP để lấy thông tin sản phẩm (giá, tên) khi tạo đơn.
 * - Timeout từ RestTemplateConfig tránh block vô hạn.
 * - 404 → Optional.empty(); timeout/5xx → ProductServiceUnavailableException.
 */
@Component
public class ProductRestClient implements ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ProductRestClient.class);

    private final RestTemplate restTemplate;
    private final ProductServiceProperties props;

    public ProductRestClient(@Qualifier("productServiceRestTemplate") RestTemplate restTemplate,
                            ProductServiceProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    private String baseUrl() {
        return props.getBaseUrl();
    }

    @Override
    public Optional<ProductInfoDto> getProductById(Long id) {
        String url = baseUrl() + "/api/products/" + id;
        try {
            ProductInfoDto body = restTemplate.getForObject(url, ProductInfoDto.class);
            return Optional.ofNullable(body);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (ResourceAccessException e) {
            log.warn("Product service unreachable for id={}: {}", id, e.getMessage());
            throw new ProductServiceUnavailableException("Product service unreachable: " + e.getMessage(), e);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.warn("Product service error for id={}: {} {}", id, e.getStatusCode(), e.getMessage());
            throw new ProductServiceUnavailableException("Product service error: " + e.getStatusCode(), e);
        }
    }

    @Override
    public List<ProductInfoDto> getProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        String idsParam = ids.stream().map(Object::toString).reduce((a, b) -> a + "," + b).orElse("");
        String url = baseUrl() + "/api/products/bulk?ids=" + idsParam;
        try {
            ResponseEntity<List<ProductInfoDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (ResourceAccessException e) {
            log.warn("Product service unreachable for bulk ids: {}", e.getMessage());
            throw new ProductServiceUnavailableException("Product service unreachable: " + e.getMessage(), e);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.warn("Product service error for bulk: {} {}", e.getStatusCode(), e.getMessage());
            throw new ProductServiceUnavailableException("Product service error: " + e.getStatusCode(), e);
        }
    }
}
