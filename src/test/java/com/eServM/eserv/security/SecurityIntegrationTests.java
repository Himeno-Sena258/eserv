package com.eServM.eserv.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/test-security.db")
class SecurityIntegrationTests {

    private static final String ADMIN_KEY = "ADMIN-KEY-1-20251230";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validAdminKeyAllowsApiAccess() throws Exception {
        mockMvc.perform(get("/api/hello").header(AdminKeyFilter.ADMIN_KEY_HEADER, ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, World!"));
    }

    @Test
    void adminKeyWithWhitespaceIsTrimmed() throws Exception {
        mockMvc.perform(get("/api/hello").header(AdminKeyFilter.ADMIN_KEY_HEADER, "  " + ADMIN_KEY + "  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, World!"));
    }

    @Test
    void missingAdminKeyIsRejected() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("无效的管理员密钥"));
    }

    @Test
    void invalidAdminKeyIsRejected() throws Exception {
        mockMvc.perform(get("/api/orders").header(AdminKeyFilter.ADMIN_KEY_HEADER, "invalid-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("无效的管理员密钥"));
    }

    @Test
    void nonApiEndpointsBypassFilter() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
    }
}
