package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.GetMerchantCommand;
import com.asterion.merchant.application.exception.MerchantOwnershipException;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetMerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;
    private GetMerchantService service;

    @BeforeEach
    void setUp() {
        service = new GetMerchantService(merchantRepository);
    }

    @Test
    void shouldReturnMerchantWhenAuthenticatedUserOwnsMerchant() {
        UUID ownerUserId = UUID.randomUUID();
        Merchant merchant = Merchant.create(
                ownerUserId,
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );

        when(merchantRepository.findById(merchant.id()))
                .thenReturn(Optional.of(merchant));

        Merchant result = service.get(
                new GetMerchantCommand(merchant.id(), ownerUserId));

        assertThat(result).isSameAs(merchant);

        verify(merchantRepository).findById(merchant.id());
        verifyNoMoreInteractions(merchantRepository);
    }

    @Test
    void shouldRejectWhenMerchantDoesNotExist() {
        UUID merchantId = UUID.randomUUID();
        UUID authenticatedUserId = UUID.randomUUID();

        when(merchantRepository.findById(merchantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service
                .get(new GetMerchantCommand(merchantId, authenticatedUserId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Merchant not found: " + merchantId);

        verify(merchantRepository).findById(merchantId);
    }

    @Test
    void shouldRejectWhenAuthenticatedUserDoesNotOwnMerchant() {
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
                .get(new GetMerchantCommand(merchant.id(), authenticatedUserId)))
                .isInstanceOf(MerchantOwnershipException.class)
                .hasMessage("Authenticated user does not own merchant: " + merchant.id());

        verify(merchantRepository).findById(merchant.id());
    }

    @Test
    void shouldRejectMissingAuthenticatedUserId() {
        UUID merchantId = UUID.randomUUID();

        assertThatThrownBy(() -> service
                .get(new GetMerchantCommand(merchantId, null)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("authenticatedUserId must not be null");

        verifyNoInteractions(merchantRepository);
    }

    @Test
    void shouldRejectNullCommand() {
        assertThatThrownBy(() -> service.get(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");

        verifyNoInteractions(merchantRepository);
    }

    @Test
    void shouldRejectNullMerchantId() {
        assertThatThrownBy(() -> service
                .get(new GetMerchantCommand(null, UUID.randomUUID())))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("merchantId must not be null");

        verifyNoInteractions(merchantRepository);
    }
}