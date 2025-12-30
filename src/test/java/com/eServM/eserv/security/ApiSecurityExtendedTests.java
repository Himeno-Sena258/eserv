package com.eServM.eserv.security;

import com.eServM.eserv.dto.OrderRequest;
import com.eServM.eserv.dto.OrderResponse;
import com.eServM.eserv.dto.ProductRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/test-security-extended.db",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=",
        "jwt.exp.minutes=60"
})
class ApiSecurityExtendedTests {

    private static final String ADMIN_KEY = "ADMIN-KEY-1-20251230";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;
    private String userCustomerUid;

    @BeforeEach
    void setup() throws Exception {
        adminToken = obtainAdminToken();
        register("extUser", "pass");
        userToken = login("extUser", "pass");
        userCustomerUid = fetchOwnCustomerUid(userToken);
    }

    // 测试点：用户订单ACL正向（3例）
    @Test
    void userCreateOwnOrderOk() throws Exception {
        String json = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(
                                "自有订单1", "商品1", userCustomerUid, OffsetDateTime.now()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        OrderResponse resp = objectMapper.readValue(json, OrderResponse.class);
        assertThat(resp.customerUid()).isEqualTo(userCustomerUid);
    }
    @Test
    void userUpdateOwnOrderOk() throws Exception {
        OrderResponse created = createOrder(userToken, "自有订单2", "商品2", userCustomerUid);
        mockMvc.perform(put("/api/orders/" + created.uid())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(
                                "自有订单2-更新", "商品2-更新", userCustomerUid, OffsetDateTime.now().plusDays(1)))))
                .andExpect(status().isOk());
    }
    @Test
    void userDeleteOwnOrderOk() throws Exception {
        OrderResponse created = createOrder(userToken, "自有订单3", "商品3", userCustomerUid);
        mockMvc.perform(delete("/api/orders/" + created.uid())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }

    // 测试点：404未找到（3例）
    @Test
    void notFoundCustomer() throws Exception {
        mockMvc.perform(get("/api/customers/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
    @Test
    void notFoundOrder() throws Exception {
        mockMvc.perform(get("/api/orders/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
    @Test
    void notFoundOrderNote() throws Exception {
        mockMvc.perform(get("/api/order-notes/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound());
    }

    // 测试点：商品请求校验400（3例）
    @Test
    void productNameBlankReturns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductRequest(
                                "", "desc", new BigDecimal("1.00"), true))))
                .andExpect(status().isBadRequest());
    }
    @Test
    void productUnitPriceNegativeReturns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductRequest(
                                "名称", "desc", new BigDecimal("-1.00"), true))))
                .andExpect(status().isBadRequest());
    }
    @Test
    void productDescriptionTooLongReturns400() throws Exception {
        String longDesc = "x".repeat(2050);
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductRequest(
                                "名称", longDesc, new BigDecimal("1.00"), true))))
                .andExpect(status().isBadRequest());
    }

    // 测试点：Bearer空白修剪（3例）
    @Test
    void paddedBearerWorksOnHello() throws Exception {
        mockMvc.perform(get("/api/hello")
                        .header("Authorization", "Bearer  " + adminToken + "  "))
                .andExpect(status().isOk());
    }
    @Test
    void paddedBearerWorksOnAdminCustomers() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer   " + adminToken + "   "))
                .andExpect(status().isOk());
    }
    @Test
    void paddedBearerWorksOnUserCustomers() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer   " + userToken + "   "))
                .andExpect(status().isOk());
    }

    private String obtainAdminToken() throws Exception {
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
    private void register(String username, String password) throws Exception {
        int status = mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getStatus();
        if (status != 201 && status != 409) {
            throw new AssertionError("注册失败，状态码: " + status);
        }
    }
    private String login(String username, String password) throws Exception {
        String resp = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int start = resp.indexOf(":\"") + 2;
        int end = resp.lastIndexOf("\"");
        return resp.substring(start, end);
    }
    private String fetchOwnCustomerUid(String token) throws Exception {
        String json = mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var arr = objectMapper.readTree(json);
        return arr.get(0).get("uid").asText();
    }
    private OrderResponse createOrder(String token, String summary, String product, String customerUid) throws Exception {
        String json = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(
                                summary, product, customerUid, OffsetDateTime.now()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, OrderResponse.class);
    }
}

