package com.asterion.merchant.api.controller;

import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import com.asterion.merchant.domain.model.MerchantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MerchantInternalControllerTest {

    private static final UUID MERCHANT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID OWNER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private MerchantRepository merchantRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        merchantRepository = mock(MerchantRepository.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new MerchantInternalController(merchantRepository))
                .build();
    }

    @Test
    void shouldReturnActiveMerchant() throws Exception {
        Merchant merchant = Merchant.reconstitute(
                MERCHANT_ID,
                OWNER_ID,
                "Test Business",
                "Test Legal Name",
                "merchant@example.com",
                MerchantStatus.ACTIVE,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));

        mockMvc.perform(get("/internal/api/v1/merchants/{merchantId}", MERCHANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId").value(MERCHANT_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(merchantRepository).findById(MERCHANT_ID);
    }

    @Test
    void shouldReturnSuspendedMerchant() throws Exception {
        Merchant merchant = Merchant.reconstitute(
                MERCHANT_ID,
                OWNER_ID,
                "Test Business",
                "Test Legal Name",
                "merchant@example.com",
                MerchantStatus.SUSPENDED,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));

        mockMvc.perform(get("/internal/api/v1/merchants/{merchantId}", MERCHANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void shouldReturnNotFoundWhenMerchantDoesNotExist() throws Exception {
        when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/internal/api/v1/merchants/{merchantId}", MERCHANT_ID))
                .andExpect(status().isNotFound());

        verify(merchantRepository).findById(MERCHANT_ID);
    }
}