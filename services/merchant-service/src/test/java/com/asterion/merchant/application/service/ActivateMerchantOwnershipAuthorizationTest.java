package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.ActivateMerchantCommand;
import com.asterion.merchant.application.exception.MerchantOwnershipException;
import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import com.asterion.merchant.domain.model.MerchantStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ActivateMerchantOwnershipAuthorizationTest {

    private MerchantRepository merchantRepository;
    private MerchantOutboxRepository merchantOutboxRepository;
    private ObjectMapper objectMapper;
    private ActivateMerchantService service;

    @BeforeEach
    void setUp() {
        merchantRepository = mock(MerchantRepository.class);
        merchantOutboxRepository = mock(MerchantOutboxRepository.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new ActivateMerchantService(
                merchantRepository, merchantOutboxRepository, objectMapper);
    }

    @Test
    void shouldActivateMerchantWhenAuthenticatedUserOwnsMerchant() {
        UUID ownerUserId = UUID.randomUUID();
        Merchant merchant = Merchant.create(
                ownerUserId,
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );

        when(merchantRepository.findById(merchant.id()))
                .thenReturn(Optional.of(merchant));

        when(merchantRepository.save(merchant))
                .thenReturn(merchant);

        Merchant result = service.activate(
                new ActivateMerchantCommand(merchant.id(), ownerUserId));

        assertThat(result).isSameAs(merchant);
        assertThat(result.status()).isEqualTo(MerchantStatus.ACTIVE);

        verify(merchantRepository).findById(merchant.id());
        verify(merchantRepository).save(merchant);
        verify(merchantOutboxRepository).save(any(MerchantOutboxEvent.class));
    }

    @Test
    void shouldRejectActivationWhenAuthenticatedUserDoesNotOwnMerchant() {
        UUID ownerUserId = UUID.randomUUID();
        UUID authenticatedUserId = UUID.randomUUID();
        Merchant merchant = Merchant.create(
                ownerUserId,
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );

        when(merchantRepository.findById(merchant.id()))
                .thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> service
                .activate(new ActivateMerchantCommand(merchant.id(), authenticatedUserId)))
                .isInstanceOf(MerchantOwnershipException.class)
                .hasMessage("Authenticated user does not own merchant: " + merchant.id());

        assertThat(merchant.status()).isEqualTo(MerchantStatus.PENDING);

        verify(merchantRepository).findById(merchant.id());
        verify(merchantRepository, never()).save(any(Merchant.class));
        verifyNoInteractions(merchantOutboxRepository);
    }

    @Test
    void shouldRejectMissingAuthenticatedUserId() {
        UUID ownerUserId = UUID.randomUUID();
        Merchant merchant = Merchant.create(
                ownerUserId,
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );

        assertThatThrownBy(() -> service
                .activate(new ActivateMerchantCommand(merchant.id(), null)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("authenticatedUserId must not be null");

        assertThat(merchant.status()).isEqualTo(MerchantStatus.PENDING);

        verifyNoInteractions(merchantRepository);
        verifyNoInteractions(merchantOutboxRepository);
    }
}