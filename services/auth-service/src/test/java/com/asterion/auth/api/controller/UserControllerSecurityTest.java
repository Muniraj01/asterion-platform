package com.asterion.auth.api.controller;

import com.asterion.auth.application.port.out.JwtTokenProvider;
import com.asterion.auth.application.port.out.UserRepository;
import com.asterion.auth.infrastructure.security.JwtAuthenticationConverter;
import com.asterion.auth.infrastructure.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfiguration.class)
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtAuthenticationConverter jwtAuthenticationConverter;

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