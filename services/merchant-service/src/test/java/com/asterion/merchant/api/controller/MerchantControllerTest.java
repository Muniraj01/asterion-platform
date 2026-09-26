package com.asterion.merchant.api.controller;

import com.asterion.merchant.application.port.in.CreateMerchantUseCase;
import com.asterion.merchant.domain.model.Merchant;
import com.asterion.merchant.domain.model.MerchantStatus;
import com.asterion.merchant.infrastructure.security.MerchantUserIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MerchantControllerTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private CreateMerchantUseCase createMerchantUseCase;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        createMerchantUseCase = mock(CreateMerchantUseCase.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();

        MerchantController controller = new MerchantController(createMerchantUseCase);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void shouldCreateMerchantUsingAuthenticatedUserIdentity() throws Exception {
        Merchant merchant = Merchant.reconstitute(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                USER_ID,
                "Acme Store",
                "Acme Technologies Pvt Ltd",
                "owner@example.com",
                MerchantStatus.PENDING,
                Instant.parse("2026-09-10T10:00:00Z")
        );

        when(createMerchantUseCase.create(any())).thenReturn(merchant);

        String requestBody = """
                {
                  "businessName": "Acme Store",
                  "legalName": "Acme Technologies Pvt Ltd",
                  "contactEmail": "owner@example.com"
                }
                """;

        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .requestAttr(MerchantUserIdentity.class.getName(),
                                new MerchantUserIdentity(USER_ID,
                                        "owner@example.com", "USER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantId")
                        .value("22222222-2222-2222-2222-222222222222"))
                .andExpect(jsonPath("$.ownerUserId")
                        .value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.businessName")
                        .value("Acme Store"))
                .andExpect(jsonPath("$.status")
                        .value("PENDING"));

        verify(createMerchantUseCase).create(argThat(command ->
                command.ownerUserId().equals(USER_ID)
                        && command.businessName().equals("Acme Store")
                        && command.legalName().equals("Acme Technologies Pvt Ltd")
                        && command.contactEmail().equals("owner@example.com")
        ));
    }

    @Test
    void shouldRejectRequestWithoutTrustedUserIdentity() throws Exception {
        String requestBody = """
                {
                  "businessName": "Acme Store",
                  "legalName": "Acme Technologies Pvt Ltd",
                  "contactEmail": "owner@example.com"
                }
                """;

        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(createMerchantUseCase);
    }

    @Test
    void shouldNotAcceptOwnerUserIdFromRequest() throws Exception {
        Merchant merchant = Merchant.reconstitute(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                USER_ID,
                "Acme Store",
                "Acme Technologies Pvt Ltd",
                "owner@example.com",
                MerchantStatus.PENDING,
                Instant.parse("2026-09-10T10:00:00Z")
        );

        when(createMerchantUseCase.create(any())).thenReturn(merchant);

        UUID attackerUserId =
                UUID.fromString("99999999-9999-9999-9999-999999999999");

        String requestBody = """
                {
                  "ownerUserId": "%s",
                  "businessName": "Acme Store",
                  "legalName": "Acme Technologies Pvt Ltd",
                  "contactEmail": "owner@example.com"
                }
                """.formatted(attackerUserId);

        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .requestAttr(MerchantUserIdentity.class.getName(),
                                new MerchantUserIdentity(USER_ID,
                                        "owner@example.com", "USER")))
                .andExpect(status().isCreated());

        verify(createMerchantUseCase).create(argThat(
                command -> command.ownerUserId().equals(USER_ID)));
    }

    @Test
    void shouldRejectBlankBusinessName() throws Exception {
        String requestBody = """
                {
                  "businessName": "",
                  "legalName": "Acme Technologies Pvt Ltd",
                  "contactEmail": "owner@example.com"
                }
                """;

        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .requestAttr(MerchantUserIdentity.class.getName(),
                                new MerchantUserIdentity(USER_ID,
                                        "owner@example.com", "USER")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createMerchantUseCase);
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        String requestBody = """
                {
                  "businessName": "Acme Store",
                  "legalName": "Acme Technologies Pvt Ltd",
                  "contactEmail": "not-an-email"
                }
                """;

        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .requestAttr(MerchantUserIdentity.class.getName(),
                                new MerchantUserIdentity(USER_ID,
                                        "owner@example.com", "USER")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createMerchantUseCase);
    }
}