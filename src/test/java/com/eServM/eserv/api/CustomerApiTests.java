package com.eServM.eserv.api;

import com.eServM.eserv.dto.CustomerRequest;
import com.eServM.eserv.dto.CustomerResponse;
import com.eServM.eserv.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        "spring.datasource.url=jdbc:sqlite:target/test-customers.db",
        "jwt.secret=MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=",
        "jwt.exp.minutes=60"
})
class CustomerApiTests {

    private static final String ADMIN_KEY = "ADMIN-KEY-1-20251230";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void clean() {
        customerRepository.deleteAll();
    }

    @Test
    void createAndReadCustomer() throws Exception {
        String token = obtainToken();
        CustomerResponse created = createCustomer(token, "客户A", "电话");

        String json = mockMvc.perform(get("/api/customers/" + created.uid())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        CustomerResponse fetched = objectMapper.readValue(json, CustomerResponse.class);
        assertThat(fetched.name()).isEqualTo("客户A");
        assertThat(fetched.contactMethod()).isEqualTo("电话");
    }

    @Test
    void updateCustomer() throws Exception {
        String token = obtainToken();
        CustomerResponse created = createCustomer(token, "客户B", "微信");

        String json = mockMvc.perform(put("/api/customers/" + created.uid())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CustomerRequest("客户B-更新", "邮箱"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        CustomerResponse updated = objectMapper.readValue(json, CustomerResponse.class);
        assertThat(updated.name()).isEqualTo("客户B-更新");
        assertThat(updated.contactMethod()).isEqualTo("邮箱");
    }

    @Test
    void deleteCustomer() throws Exception {
        String token = obtainToken();
        CustomerResponse created = createCustomer(token, "客户C", "短信");

        mockMvc.perform(delete("/api/customers/" + created.uid())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/customers/" + created.uid())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingAdminKeyReturns401() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());
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
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int start = response.indexOf(":\"") + 2;
        int end = response.lastIndexOf("\"");
        return response.substring(start, end);
    }
}
