package com.eServM.eserv.api;

import com.eServM.eserv.dto.CustomerRequest;
import com.eServM.eserv.dto.CustomerResponse;
import com.eServM.eserv.dto.OrderNoteRequest;
import com.eServM.eserv.dto.OrderNoteResponse;
import com.eServM.eserv.dto.OrderRequest;
import com.eServM.eserv.dto.OrderResponse;
import com.eServM.eserv.repository.CustomerOrderRepository;
import com.eServM.eserv.repository.CustomerRepository;
import com.eServM.eserv.repository.OrderNoteRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
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
        "spring.datasource.url=jdbc:sqlite:target/test-order-notes.db",
        "jwt.secret=MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=",
        "jwt.exp.minutes=60"
})
class OrderNoteApiTests {

    private static final String ADMIN_KEY = "ADMIN-KEY-1-20251230";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderNoteRepository orderNoteRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void clean() {
        orderNoteRepository.deleteAll();
        customerOrderRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void createAndFetchNote() throws Exception {
        String token = obtainToken();
        OrderResponse order = createOrderFlow(token);

        OrderNoteResponse created = createOrderNote(token, order.uid(), "首次联系");

        String json = mockMvc.perform(get("/api/order-notes/" + created.uid())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        OrderNoteResponse fetched = objectMapper.readValue(json, OrderNoteResponse.class);
        assertThat(fetched.orderUid()).isEqualTo(order.uid());
        assertThat(fetched.message()).isEqualTo("首次联系");
    }

    @Test
    void listNotesByOrder() throws Exception {
        String token = obtainToken();
        OrderResponse order = createOrderFlow(token);
        createOrderNote(token, order.uid(), "备注1");
        createOrderNote(token, order.uid(), "备注2");

        String json = mockMvc.perform(get("/api/order-notes")
                .param("orderUid", order.uid())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<OrderNoteResponse> notes = objectMapper.readValue(json, new TypeReference<>() {
        });
        assertThat(notes).hasSize(2);
    }

    @Test
    void updateNote() throws Exception {
        String token = obtainToken();
        OrderResponse order = createOrderFlow(token);
        OrderNoteResponse created = createOrderNote(token, order.uid(), "旧备注");

        String json = mockMvc.perform(put("/api/order-notes/" + created.uid())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OrderNoteRequest(order.uid(), "新备注"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        OrderNoteResponse updated = objectMapper.readValue(json, OrderNoteResponse.class);
        assertThat(updated.message()).isEqualTo("新备注");
    }

    @Test
    void deleteNote() throws Exception {
        String token = obtainToken();
        OrderResponse order = createOrderFlow(token);
        OrderNoteResponse created = createOrderNote(token, order.uid(), "删除测试");

        mockMvc.perform(delete("/api/order-notes/" + created.uid())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/order-notes/" + created.uid())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingAdminKeyIsRejected() throws Exception {
        mockMvc.perform(get("/api/order-notes"))
                .andExpect(status().isUnauthorized());
    }

    private OrderNoteResponse createOrderNote(String token, String orderUid, String message) throws Exception {
        String json = mockMvc.perform(post("/api/order-notes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OrderNoteRequest(orderUid, message))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, OrderNoteResponse.class);
    }

    private OrderResponse createOrderFlow(String token) throws Exception {
        CustomerResponse customer = createCustomer(token, "订单客户", "邮箱");
        String json = mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OrderRequest(
                        "首单",
                        "商品X",
                        customer.uid(),
                        OffsetDateTime.now()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, OrderResponse.class);
    }

    private CustomerResponse createCustomer(String token, String name, String contact) throws Exception {
        String json = mockMvc.perform(post("/api/customers")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CustomerRequest(name, contact))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, CustomerResponse.class);
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
