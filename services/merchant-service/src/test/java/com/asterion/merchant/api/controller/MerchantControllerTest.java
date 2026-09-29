package com.asterion.merchant.api.controller;

import com.asterion.merchant.application.exception.MerchantOwnershipException;
import com.asterion.merchant.application.port.in.ActivateMerchantUseCase;
import com.asterion.merchant.application.port.in.CreateMerchantUseCase;
import com.asterion.merchant.application.port.in.GetMerchantUseCase;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MerchantControllerTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID OTHER_USER_ID =
            UUID.fromString("99999999-9999-9999-9999-999999999999");

    private static final UUID MERCHANT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private CreateMerchantUseCase createMerchantUseCase;
    private ActivateMerchantUseCase activateMerchantUseCase;
    private GetMerchantUseCase getMerchantUseCase;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        createMerchantUseCase = mock(CreateMerchantUseCase.class);
        activateMerchantUseCase = mock(ActivateMerchantUseCase.class);
        getMerchantUseCase = mock(GetMerchantUseCase.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        MerchantController controller = new MerchantController(
                createMerchantUseCase, activateMerchantUseCase, getMerchantUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // -------------------------------------------------------------------------
    // G1 - Merchant creation
    // -------------------------------------------------------------------------

    @Test
    void shouldCreateMerchantUsingAuthenticatedUserIdentity() throws Exception {
        Merchant merchant = Merchant.reconstitute(
                MERCHANT_ID,
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
                .andExpect(jsonPath("$.merchantId").value(MERCHANT_ID.toString()))
                .andExpect(jsonPath("$.ownerUserId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.businessName").value("Acme Store"))
                .andExpect(jsonPath("$.status").value("PENDING"));

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
                MERCHANT_ID,
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
                  "ownerUserId": "%s",
                  "businessName": "Acme Store",
                  "legalName": "Acme Technologies Pvt Ltd",
                  "contactEmail": "owner@example.com"
                }
                """.formatted(OTHER_USER_ID);

        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .requestAttr(MerchantUserIdentity.class.getName(),
                                new MerchantUserIdentity(
                                        USER_ID, "owner@example.com", "USER")))
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
                                new MerchantUserIdentity(
                                        USER_ID, "owner@example.com", "USER")))
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
                                new MerchantUserIdentity(
                                        USER_ID, "owner@example.com", "USER")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createMerchantUseCase);
    }


    // -------------------------------------------------------------------------
    // G2 - Merchant activation
    // -------------------------------------------------------------------------

    @Test
    void shouldActivateMerchantUsingAuthenticatedUserIdentity() throws Exception {
        Merchant merchant = Merchant.reconstitute(
                MERCHANT_ID,
                USER_ID,
                "Acme Store",
                "Acme Technologies Pvt Ltd",
                "owner@example.com",
                MerchantStatus.ACTIVE,
                Instant.parse("2026-09-10T10:00:00Z")
        );

        when(activateMerchantUseCase.activate(any())).thenReturn(merchant);

        mockMvc.perform(post("/api/v1/merchants/{merchantId}/activate", MERCHANT_ID)
                        .requestAttr(MerchantUserIdentity.class.getName(),
                                new MerchantUserIdentity(
                                        USER_ID, "owner@example.com", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId").value(MERCHANT_ID.toString()))
                .andExpect(jsonPath("$.ownerUserId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.businessName").value("Acme Store"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(activateMerchantUseCase).activate(argThat(command ->
                command.merchantId().equals(MERCHANT_ID) &&
                        command.authenticatedUserId().equals(USER_ID)));
    }

    @Test
    void shouldRejectActivationWithoutTrustedUserIdentity() throws Exception {
        mockMvc.perform(post("/api/v1/merchants/{merchantId}/activate", MERCHANT_ID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(activateMerchantUseCase);
    }

    @Test
    void shouldRejectActivationWhenAuthenticatedUserDoesNotOwnMerchant() throws Exception {
        when(activateMerchantUseCase.activate(any()))
                .thenThrow(new MerchantOwnershipException(
                        "Authenticated user does not own merchant: " + MERCHANT_ID));

        mockMvc.perform(post("/api/v1/merchants/{merchantId}/activate", MERCHANT_ID)
                        .requestAttr(MerchantUserIdentity.class.getName(),
                                new MerchantUserIdentity(OTHER_USER_ID,
                                        "attacker@example.com", "USER")))
                .andExpect(status().isForbidden());

        verify(activateMerchantUseCase).activate(argThat(command ->
                command.merchantId().equals(MERCHANT_ID) &&
                        command.authenticatedUserId().equals(OTHER_USER_ID)));
    }

    @Test
    void shouldReturnNotFoundWhenMerchantDoesNotExist() throws Exception {
        when(activateMerchantUseCase.activate(any()))
                .thenThrow(new IllegalArgumentException("Merchant not found: " + MERCHANT_ID));

        mockMvc.perform(post("/api/v1/merchants/{merchantId}/activate", MERCHANT_ID)
                        .requestAttr(MerchantUserIdentity.class.getName(),
                                new MerchantUserIdentity(USER_ID,
                                        "owner@example.com", "USER")))
                .andExpect(status().isNotFound());

        verify(activateMerchantUseCase).activate(argThat(command ->
                command.merchantId().equals(MERCHANT_ID) &&
                        command.authenticatedUserId().equals(USER_ID)));
    }

    @Test
    void shouldReturnConflictWhenMerchantIsAlreadyActive() throws Exception {
        when(activateMerchantUseCase.activate(any()))
                .thenThrow(new IllegalStateException("Only a pending merchant can be activated"));

        mockMvc.perform(post("/api/v1/merchants/{merchantId}/activate", MERCHANT_ID)
                        .requestAttr(MerchantUserIdentity.class.getName(),
                                new MerchantUserIdentity(USER_ID,
                                        "owner@example.com", "USER")))
                .andExpect(status().isConflict());

        verify(activateMerchantUseCase).activate(argThat(command ->
                command.merchantId().equals(MERCHANT_ID) &&
                        command.authenticatedUserId().equals(USER_ID)));
    }

    // -------------------------------------------------------------------------
    // G3 - Merchant Retrieval
    // -------------------------------------------------------------------------

    @Test
    void shouldGetMerchantUsingAuthenticatedUserIdentity() throws Exception {
        Merchant merchant = Merchant.create(
                USER_ID,
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );

        when(getMerchantUseCase.get(any())).thenReturn(merchant);

        mockMvc.perform(get("/api/v1/merchants/{merchantId}", MERCHANT_ID)
                        .requestAttr(MerchantUserIdentity.class.getName(),
                                new MerchantUserIdentity(
                                        USER_ID, "user@example.com", "USER"))
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId").value(merchant.id().toString()))
                .andExpect(jsonPath("$.ownerUserId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.businessName")
                        .value("Asterion Technologies"));

        verify(getMerchantUseCase).get(argThat(command ->
                command.merchantId().equals(MERCHANT_ID) &&
                        command.authenticatedUserId().equals(USER_ID)));
    }

    @Test
    void shouldRejectGetWithoutTrustedUserIdentity() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/{merchantId}", MERCHANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(getMerchantUseCase);
    }

    @Test
    void shouldReturnForbiddenWhenUserDoesNotOwnMerchant() throws Exception {
        when(getMerchantUseCase.get(any()))
                .thenThrow(new MerchantOwnershipException(
                        "Authenticated user does not own merchant: " + MERCHANT_ID));

        mockMvc.perform(get("/api/v1/merchants/{merchantId}", MERCHANT_ID)
                        .requestAttr(MerchantUserIdentity.class.getName(),
                                new MerchantUserIdentity(
                                        OTHER_USER_ID, "other@example.com", "USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(getMerchantUseCase).get(argThat(command ->
                command.merchantId().equals(MERCHANT_ID) &&
                        command.authenticatedUserId().equals(OTHER_USER_ID)));
    }

    @Test
    void shouldReturnNotFoundWhenMerchantDoesNotExistWhileRetrieving() throws Exception {
        when(getMerchantUseCase.get(any())).thenThrow(
                new IllegalArgumentException("Merchant not found: " + MERCHANT_ID));

        mockMvc.perform(get("/api/v1/merchants/{merchantId}", MERCHANT_ID)
                        .requestAttr(MerchantUserIdentity.class.getName(),
                                new MerchantUserIdentity(
                                        USER_ID, "user@example.com", "USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(getMerchantUseCase).get(argThat(command ->
                command.merchantId().equals(MERCHANT_ID) &&
                        command.authenticatedUserId().equals(USER_ID)));
    }

}