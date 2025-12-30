package com.eServM.eserv.order;

import com.eServM.eserv.dto.CustomerRequest;
import com.eServM.eserv.dto.CustomerResponse;
import com.eServM.eserv.dto.OrderRequest;
import com.eServM.eserv.dto.OrderResponse;
import com.eServM.eserv.repository.CustomerOrderRepository;
import com.eServM.eserv.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
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
        "spring.datasource.url=jdbc:sqlite:target/test-orders.db",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=",
        "jwt.exp.minutes=60"
})
class OrderServiceTests {

    private static final String ADMIN_KEY = "ADMIN-KEY-1-20251230";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    private CustomerResponse customer;

    @BeforeEach
    void setup() throws Exception {
        customerOrderRepository.deleteAll();
        customerRepository.deleteAll();
        String token = obtainToken();
        String response = mockMvc.perform(post("/api/customers")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CustomerRequest("测试客户", "电话联系"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        customer = objectMapper.readValue(response, CustomerResponse.class);
    }

    @Test
    void createAndFetchOrder() throws Exception {
        String token = obtainToken();
        OrderResponse created = createOrder(token, "首单", "商品A", OffsetDateTime.now());

        String json = mockMvc.perform(get("/api/orders/" + created.uid())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        OrderResponse fetched = objectMapper.readValue(json, OrderResponse.class);
        assertThat(fetched.summary()).isEqualTo("首单");
        assertThat(fetched.productName()).isEqualTo("商品A");
        assertThat(fetched.customerUid()).isEqualTo(customer.uid());
    }

    @Test
    void updateOrder() throws Exception {
        String token = obtainToken();
        OrderResponse created = createOrder(token, "旧简介", "旧商品", OffsetDateTime.now());

        String json = mockMvc.perform(put("/api/orders/" + created.uid())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OrderRequest(
                        "新简介",
                        "新商品",
                        customer.uid(),
                        OffsetDateTime.now().plusDays(1)))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        OrderResponse updated = objectMapper.readValue(json, OrderResponse.class);
        assertThat(updated.summary()).isEqualTo("新简介");
        assertThat(updated.productName()).isEqualTo("新商品");
        assertThat(updated.orderTime()).isAfter(created.orderTime());
    }

    @Test
    void deleteOrder() throws Exception {
        String token = obtainToken();
        OrderResponse created = createOrder(token, "删除测试", "商品C", OffsetDateTime.now());

        mockMvc.perform(delete("/api/orders/" + created.uid())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/orders/" + created.uid())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestWithoutAdminKeyIsRejected() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    private OrderResponse createOrder(String token, String summary, String product, OffsetDateTime orderTime) throws Exception {
        String json = mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OrderRequest(
                        summary,
                        product,
                        customer.uid(),
                        orderTime))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, OrderResponse.class);
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
