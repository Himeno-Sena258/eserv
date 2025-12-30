package com.eServM.eserv.order;

import com.eServM.eserv.dto.CustomerRequest;
import com.eServM.eserv.dto.CustomerResponse;
import com.eServM.eserv.dto.OrderRequest;
import com.eServM.eserv.dto.OrderResponse;
import com.eServM.eserv.repository.CustomerOrderRepository;
import com.eServM.eserv.repository.CustomerRepository;
import com.eServM.eserv.security.AdminKeyFilter;
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
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/test-orders.db")
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
        String response = mockMvc.perform(post("/api/customers")
                .header(AdminKeyFilter.ADMIN_KEY_HEADER, ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CustomerRequest("测试客户", "电话联系"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        customer = objectMapper.readValue(response, CustomerResponse.class);
    }

    @Test
    void createAndFetchOrder() throws Exception {
        OrderResponse created = createOrder("首单", "商品A", OffsetDateTime.now());

        String json = mockMvc.perform(get("/api/orders/" + created.uid())
                .header(AdminKeyFilter.ADMIN_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        OrderResponse fetched = objectMapper.readValue(json, OrderResponse.class);
        assertThat(fetched.summary()).isEqualTo("首单");
        assertThat(fetched.productName()).isEqualTo("商品A");
        assertThat(fetched.customerUid()).isEqualTo(customer.uid());
    }

    @Test
    void updateOrder() throws Exception {
        OrderResponse created = createOrder("旧简介", "旧商品", OffsetDateTime.now());

        String json = mockMvc.perform(put("/api/orders/" + created.uid())
                .header(AdminKeyFilter.ADMIN_KEY_HEADER, ADMIN_KEY)
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
        OrderResponse created = createOrder("删除测试", "商品C", OffsetDateTime.now());

        mockMvc.perform(delete("/api/orders/" + created.uid())
                .header(AdminKeyFilter.ADMIN_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/orders/" + created.uid())
                .header(AdminKeyFilter.ADMIN_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestWithoutAdminKeyIsRejected() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    private OrderResponse createOrder(String summary, String product, OffsetDateTime orderTime) throws Exception {
        String json = mockMvc.perform(post("/api/orders")
                .header(AdminKeyFilter.ADMIN_KEY_HEADER, ADMIN_KEY)
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
}
