package com.eServM.eserv.security;

import com.eServM.eserv.dto.CustomerRequest;
import com.eServM.eserv.dto.OrderNoteRequest;
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
        "spring.datasource.url=jdbc:sqlite:target/test-order-note-acl.db",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=",
        "jwt.exp.minutes=60"
})
class OrderNoteAclTests {

    private static final String ADMIN_KEY = "ADMIN-KEY-1-20251230";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;
    private String otherUserToken;
    private String ownOrderUid;
    private String foreignOrderUid;

    @BeforeEach
    void setup() throws Exception {
        adminToken = obtainAdminToken();
        register("noteUser", "pass");
        register("otherUser", "pass2");
        userToken = login("noteUser", "pass");
        otherUserToken = login("otherUser", "pass2");
        String ownCustomerUid = fetchOwnCustomerUid(userToken);
        String foreignCustomerUid = fetchOwnCustomerUid(otherUserToken);
        ownOrderUid = createOrder(userToken, "自有订单", "商品X", ownCustomerUid).uid();
        foreignOrderUid = createOrder(otherUserToken, "外部订单", "商品Y", foreignCustomerUid).uid();
    }

    // 测试点：订单备注所有者约束（3例）
    @Test
    void userCanListOwnOrderNotes() throws Exception {
        createNoteAsAdmin(ownOrderUid, "自有备注1");
        String json = mockMvc.perform(get("/api/order-notes")
                        .header("Authorization", "Bearer " + userToken)
                        .param("orderUid", ownOrderUid))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var arr = objectMapper.readTree(json);
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isEqualTo(1);
    }

    @Test
    void userCannotListForeignOrderNotes() throws Exception {
        createNoteAsAdmin(foreignOrderUid, "外部备注1");
        mockMvc.perform(get("/api/order-notes")
                        .header("Authorization", "Bearer " + userToken)
                        .param("orderUid", foreignOrderUid))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotCreateNoteForForeignOrder() throws Exception {
        mockMvc.perform(post("/api/order-notes")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderNoteRequest(foreignOrderUid, "越权备注"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void userListWithoutParamShowsOwnOnly() throws Exception {
        createNoteAsAdmin(ownOrderUid, "自有备注2");
        createNoteAsAdmin(foreignOrderUid, "外部备注2");
        String json = mockMvc.perform(get("/api/order-notes")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var arr = objectMapper.readTree(json);
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void adminListWithoutParamShowsAll() throws Exception {
        createNoteAsAdmin(ownOrderUid, "自有备注3");
        createNoteAsAdmin(foreignOrderUid, "外部备注3");
        String json = mockMvc.perform(get("/api/order-notes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var arr = objectMapper.readTree(json);
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void adminListWithOrderUidFilters() throws Exception {
        createNoteAsAdmin(ownOrderUid, "自有备注4");
        String json = mockMvc.perform(get("/api/order-notes")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("orderUid", ownOrderUid))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var arr = objectMapper.readTree(json);
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void userCanReadOwnNoteByUid() throws Exception {
        String noteUid = createNoteAsUser(ownOrderUid, "自有备注5");
        mockMvc.perform(get("/api/order-notes/" + noteUid)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotReadForeignNoteByUid() throws Exception {
        String noteUid = createNoteAsUser(foreignOrderUid, "外部备注5");
        mockMvc.perform(get("/api/order-notes/" + noteUid)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadForeignNoteByUid() throws Exception {
        String noteUid = createNoteAsUser(foreignOrderUid, "外部备注6");
        mockMvc.perform(get("/api/order-notes/" + noteUid)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void userUpdateOwnNoteToForeignOrderForbidden() throws Exception {
        String noteUid = createNoteAsUser(ownOrderUid, "自有备注7");
        mockMvc.perform(put("/api/order-notes/" + noteUid)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderNoteRequest(foreignOrderUid, "更新到外部"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void userUpdateForeignNoteToOwnOrderForbidden() throws Exception {
        String noteUid = createNoteAsUser(foreignOrderUid, "外部备注7");
        mockMvc.perform(put("/api/order-notes/" + noteUid)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderNoteRequest(ownOrderUid, "更新到自有"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUpdateForeignNoteToOwnOrderOk() throws Exception {
        String noteUid = createNoteAsUser(foreignOrderUid, "外部备注8");
        String json = mockMvc.perform(put("/api/order-notes/" + noteUid)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderNoteRequest(ownOrderUid, "管理员更新到自有"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var node = objectMapper.readTree(json);
        assertThat(node.get("orderUid").asText()).isEqualTo(ownOrderUid);
    }

    @Test
    void userDeleteOwnNoteOk() throws Exception {
        String noteUid = createNoteAsUser(ownOrderUid, "自有删除1");
        mockMvc.perform(delete("/api/order-notes/" + noteUid)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void userDeleteForeignNoteForbidden() throws Exception {
        String noteUid = createNoteAsUser(foreignOrderUid, "外部删除1");
        mockMvc.perform(delete("/api/order-notes/" + noteUid)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminDeleteForeignNoteOk() throws Exception {
        String noteUid = createNoteAsUser(foreignOrderUid, "外部删除2");
        mockMvc.perform(delete("/api/order-notes/" + noteUid)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }
    // 测试点：订单备注非法UID触发400（3例）
    @Test
    void invalidNoteUidReadReturns400() throws Exception {
        mockMvc.perform(get("/api/order-notes/invalid-uid")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidNoteUidUpdateReturns400() throws Exception {
        mockMvc.perform(put("/api/order-notes/invalid-uid")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderNoteRequest(ownOrderUid, "更新"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidNoteUidDeleteReturns400() throws Exception {
        mockMvc.perform(delete("/api/order-notes/invalid-uid")
                        .header("Authorization", "Bearer " + adminToken))
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

    private OrderResponse createOrder(String token, String summary, String product, String customerUid) throws Exception {
        String json = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summary\":\"" + summary + "\",\"productName\":\"" + product + "\",\"customerUid\":\"" + customerUid + "\",\"orderTime\":\"" + OffsetDateTime.now().toString() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, OrderResponse.class);
    }

    private void createNoteAsAdmin(String orderUid, String message) throws Exception {
        mockMvc.perform(post("/api/order-notes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderNoteRequest(orderUid, message))))
                .andExpect(status().isCreated());
    }

    private String createNoteAsUser(String orderUid, String message) throws Exception {
        String json = mockMvc.perform(post("/api/order-notes")
                        .header("Authorization", "Bearer " + (orderUid.equals(ownOrderUid) ? userToken : otherUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderNoteRequest(orderUid, message))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("uid").asText();
    }
}
