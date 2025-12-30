package com.eServM.eserv.security;

import com.eServM.eserv.dto.CustomerRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/test-security-more.db",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=",
        "jwt.exp.minutes=60"
})
class ApiSecurityMoreTests {

    private static final String ADMIN_KEY = "ADMIN-KEY-1-20251230";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setup() throws Exception {
        adminToken = obtainAdminToken();
        register("securityUser", "pass");
        userToken = login("securityUser", "pass");
    }

    // 测试点1：401未授权（至少三个实例）
    @Test
    void unauthorizedMissingHeader() throws Exception {
        mockMvc.perform(get("/api/customers")).andExpect(status().isUnauthorized());
    }
    @Test
    void unauthorizedNonBearerScheme() throws Exception {
        mockMvc.perform(get("/api/orders").header("Authorization", "Token " + adminToken))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void unauthorizedInvalidToken() throws Exception {
        mockMvc.perform(get("/api/orders").header("Authorization", "Bearer " + "invalid.token.value"))
                .andExpect(status().isUnauthorized());
    }

    // 测试点2：登录/注册入口（至少三个实例）
    @Test
    void registerMissingUsernameReturns400() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void registerMissingPasswordReturns400() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void loginWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"securityUser\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    // 测试点3：用户角色对Customer写操作的403（至少三个实例）
    @Test
    void userCannotCreateCustomer() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CustomerRequest("X", "C"))))
                .andExpect(status().isForbidden());
    }
    @Test
    void userCannotUpdateCustomer() throws Exception {
        String customerUid = createCustomerAsAdmin("客户端A", "电话");
        mockMvc.perform(put("/api/customers/" + customerUid)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CustomerRequest("更新A", "邮箱"))))
                .andExpect(status().isForbidden());
    }
    @Test
    void userCannotDeleteCustomer() throws Exception {
        String customerUid = createCustomerAsAdmin("客户端B", "微信");
        mockMvc.perform(delete("/api/customers/" + customerUid)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // 测试点4：客户读取的所有者约束（至少三个实例）
    @Test
    void userListOwnCustomersOnly() throws Exception {
        String json = mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var arr = objectMapper.readTree(json);
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isEqualTo(1);
    }
    @Test
    void userCanGetOwnCustomerByUid() throws Exception {
        String uid = fetchOwnCustomerUid(userToken);
        mockMvc.perform(get("/api/customers/" + uid)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }
    @Test
    void userCannotGetForeignCustomerByUid() throws Exception {
        String foreignUid = createCustomerAsAdmin("外部客户", "短信");
        mockMvc.perform(get("/api/customers/" + foreignUid)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // 测试点5：非法UID触发400（至少三个实例）
    @Test
    void invalidCustomerUidReturns400() throws Exception {
        mockMvc.perform(get("/api/customers/invalid-uid")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }
    @Test
    void invalidOrderUidReadReturns400() throws Exception {
        mockMvc.perform(get("/api/orders/invalid-uid")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }
    @Test
    void invalidOrderUidUpdateReturns400() throws Exception {
        String ownCustomerUid = fetchOwnCustomerUid(userToken);
        mockMvc.perform(put("/api/orders/invalid-uid")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(
                                "x", "y", ownCustomerUid, OffsetDateTime.now()))))
                .andExpect(status().isBadRequest());
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

    private String createCustomerAsAdmin(String name, String contact) throws Exception {
        String json = mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CustomerRequest(name, contact))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("uid").asText();
    }

    private String fetchOwnCustomerUid(String token) throws Exception {
        String json = mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var arr = objectMapper.readTree(json);
        return arr.get(0).get("uid").asText();
    }
}

