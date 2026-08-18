package com.asterion.auth.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowUserToAccessCurrentUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .with(user("user@example.com")
                                .roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUserFromAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/admin")
                        .with(user("user@example.com")
                                .roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/admin")
                        .with(user("admin@example.com")
                                .roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAdminUserToAccessBothEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .with(user("admin@example.com")
                                .roles("ADMIN", "USER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/admin")
                        .with(user("admin@example.com")
                                .roles("ADMIN", "USER")))
                .andExpect(status().isOk());
    }
}