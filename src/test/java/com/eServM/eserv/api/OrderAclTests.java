package com.eServM.eserv.api;

import com.eServM.eserv.dto.OrderRequest;
import com.eServM.eserv.dto.OrderResponse;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/test-order-acl.db",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=",
        "jwt.exp.minutes=60"
})
class OrderAclTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userAToken;
    private String userBToken;
    private String customerAUid;
    private String customerBUid;

    @BeforeEach
    void setup() throws Exception {
        register("userA", "passA");
        register("userB", "passB");
        userAToken = login("userA", "passA");
        userBToken = login("userB", "passB");
        customerAUid = fetchOwnCustomerUid(userAToken);
        customerBUid = fetchOwnCustomerUid(userBToken);
    }

    @Test
    void userCanManageOwnOrdersOnly() throws Exception {
        String json = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(
                                "A的订单",
                                "商品A",
                                customerAUid,
                                OffsetDateTime.now()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        OrderResponse ownOrder = objectMapper.readValue(json, OrderResponse.class);

        mockMvc.perform(get("/api/orders/" + ownOrder.uid())
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/orders/" + ownOrder.uid())
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(
                                "A的订单-更新",
                                "商品A2",
                                customerAUid,
                                OffsetDateTime.now().plusDays(1)))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/orders/" + ownOrder.uid())
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void userCannotManageOthersOrdersOrUseOthersCustomer() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(
                                "非法订单",
                                "商品X",
                                customerBUid,
                                OffsetDateTime.now()))))
                .andExpect(status().isForbidden());
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
        if (arr.isArray() && arr.size() > 0) {
            return arr.get(0).get("uid").asText();
        }
        throw new IllegalStateException("未找到用户的客户");
    }
}
