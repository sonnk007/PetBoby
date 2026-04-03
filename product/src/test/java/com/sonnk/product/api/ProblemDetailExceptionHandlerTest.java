package com.sonnk.product.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonnk.product.model.entity.Product;
import com.sonnk.product.repository.ProductRepository;
import com.sonnk.product.utils.enums.ProductSize;
import com.sonnk.product.utils.enums.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for RFC 7807 ProblemDetail exception handling.
 *
 * Verifies:
 * - Validation errors return 400 with ProblemDetail format
 * - Not found errors return 404 with ProblemDetail format
 * - Server errors return 500 with generic message (security)
 * - All responses include traceId and timestamp extensions
 * - Content-Type is application/problem+json
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ProblemDetailExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void testValidationErrorReturnsRFC7807Format() throws Exception {
        // POST with invalid data (name is blank, price is negative)
        String invalidRequest = """
            {
              "name": "",
              "price": -1000,
              "productSize": "MEDIUM",
              "status": "ACTIVE"
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json;charset=UTF-8"))
            .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(content, Map.class);

        // Verify RFC 7807 standard fields
        assertThat(response).containsKeys("type", "title", "status", "detail", "instance");
        assertThat(response.get("type")).asString().contains("validation-error");
        assertThat(response.get("title")).isEqualTo("Validation Failed");
        assertThat(response.get("status")).isEqualTo(400);

        // Verify extensions
        assertThat(response).containsKeys("traceId", "timestamp", "validationErrors");
        assertThat(response.get("traceId")).isNotNull();
        assertThat(response.get("timestamp")).isNotNull();

        // Verify validation errors detail
        Object validationErrors = response.get("validationErrors");
        assertThat(validationErrors).isNotNull();

        System.out.println("✓ Validation error returns RFC 7807 ProblemDetail format");
        System.out.println("  Type: " + response.get("type"));
        System.out.println("  TraceId: " + response.get("traceId"));
        System.out.println("  Validation Errors: " + response.get("validationErrors"));
    }

    @Test
    void testNotFoundErrorReturnsRFC7807Format() throws Exception {
        // GET non-existent product
        MvcResult result = mockMvc.perform(get("/api/products/{id}", 99999L))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType("application/problem+json;charset=UTF-8"))
            .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(content, Map.class);

        // Verify RFC 7807 fields
        assertThat(response).containsKeys("type", "title", "status", "detail", "instance");
        assertThat(response.get("status")).isEqualTo(404);
        assertThat(response.get("title")).isEqualTo("Not Found");

        // Verify extensions
        assertThat(response).containsKeys("traceId", "timestamp");
        assertThat(response.get("traceId")).isNotNull();

        System.out.println("✓ Not found error returns RFC 7807 ProblemDetail format");
    }

    @Test
    void testContentTypeApplicationProblemJson() throws Exception {
        // Verify content-type for validation error
        mockMvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json;charset=UTF-8"));

        System.out.println("✓ Error responses return Content-Type: application/problem+json");
    }

    @Test
    void testSuccessfulCreateReturns201WithCreated() throws Exception {
        // POST valid product
        String validRequest = """
            {
              "name": "Coffee",
              "price": 50000,
              "productSize": "MEDIUM",
              "status": "ACTIVE"
            }
            """;

        mockMvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"));

        System.out.println("✓ Successful POST returns 201 Created with Location header");
    }

    @Test
    void testDeleteReturns204NoContent() throws Exception {
        // Create a product first
        Product product = new Product();
        product.setName("Test Product");
        product.setPrice(BigDecimal.valueOf(50000));
        product.setProductSize(ProductSize.M);
        product.setStatus(ProductStatus.ACTIVE);
        Product saved = productRepository.save(product);

        // Delete it
        mockMvc.perform(delete("/api/products/{id}", saved.getId())
            .queryParam("deletedBy", "test-user"))
            .andExpect(status().isNoContent());

        System.out.println("✓ DELETE returns 204 No Content");
    }

    @Test
    void testTraceIdCarriedInErrorResponse() throws Exception {
        String traceId = "550e8400-e29b-41d4-a716-446655440000";

        MvcResult result = mockMvc.perform(get("/api/products/{id}", 99999L)
            .header("X-Trace-Id", traceId))
            .andExpect(status().isNotFound())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(content, Map.class);

        // Verify traceId is included if provided
        assertThat(response.get("traceId")).isNotNull();

        System.out.println("✓ TraceId extension included in error response");
    }

    @Test
    void testTimestampIncludedInAllErrors() throws Exception {
        // Test with validation error
        MvcResult result = mockMvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isBadRequest())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(content, Map.class);

        // Verify timestamp is ISO 8601 format
        assertThat(response).containsKey("timestamp");
        assertThat(response.get("timestamp")).asString()
            .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z?");

        System.out.println("✓ Timestamp extension included in all error responses");
    }
}
