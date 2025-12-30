package com.eServM.eserv.api;

import com.eServM.eserv.dto.ProductRequest;
import com.eServM.eserv.dto.ProductResponse;
import com.eServM.eserv.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/test-products.db",
        "jwt.secret=MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=",
        "jwt.exp.minutes=60"
})
class ProductApiTests {

    private static final String ADMIN_KEY = "ADMIN-KEY-1-20251230";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void clean() {
        productRepository.deleteAll();
    }

    @Test
    void createAndFetchProduct() throws Exception {
        String token = obtainToken();
        ProductResponse created = createProduct(token, "商品A", "描述A", new BigDecimal("19.99"));

        String json = mockMvc.perform(get("/api/products/" + created.uid())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ProductResponse fetched = objectMapper.readValue(json, ProductResponse.class);
        assertThat(fetched.name()).isEqualTo("商品A");
        assertThat(fetched.unitPrice()).isEqualByComparingTo("19.99");
    }

    @Test
    void updateProduct() throws Exception {
        String token = obtainToken();
        ProductResponse created = createProduct(token, "商品B", "描述B", new BigDecimal("29.00"));

        String json = mockMvc.perform(put("/api/products/" + created.uid())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ProductRequest(
                        "商品B-更新",
                        "新版描述",
                        new BigDecimal("35.50"),
                        Boolean.FALSE))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ProductResponse updated = objectMapper.readValue(json, ProductResponse.class);
        assertThat(updated.name()).isEqualTo("商品B-更新");
        assertThat(updated.unitPrice()).isEqualByComparingTo("35.50");
        assertThat(updated.active()).isFalse();
    }

    @Test
    void deleteProduct() throws Exception {
        String token = obtainToken();
        ProductResponse created = createProduct(token, "商品C", "描述C", new BigDecimal("9.99"));

        mockMvc.perform(delete("/api/products/" + created.uid())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + created.uid())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingAdminKeyIsRejected() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    private ProductResponse createProduct(String token, String name, String description, BigDecimal price) throws Exception {
        String json = mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ProductRequest(name, description, price, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, ProductResponse.class);
    }

    private String obtainToken() throws Exception {
        String json = "{\"adminKey\":\"" + ADMIN_KEY + "\"}";
        String response = mockMvc.perform(post("/api/login/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int start = response.indexOf(":\"") + 2;
        int end = response.lastIndexOf("\"");
        return response.substring(start, end);
    }
}
