package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.ListMerchantsCommand;
import com.asterion.merchant.application.model.MerchantPage;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListMerchantsServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    private ListMerchantsService service;

    @BeforeEach
    void setUp() {
        service = new ListMerchantsService(merchantRepository);
    }

    @Test
    void shouldReturnMerchantsForAuthenticatedUser() {
        UUID ownerUserId = UUID.randomUUID();
        Merchant merchant = Merchant.create(
                ownerUserId,
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );

        MerchantPage expected = new MerchantPage(
                List.of(merchant), 0, 20, 1, 1
        );

        when(merchantRepository
                .findByOwnerUserId(ownerUserId, 0, 20))
                .thenReturn(expected);

        MerchantPage result = service.list(
                new ListMerchantsCommand(ownerUserId, 0, 20)
        );

        assertThat(result).isSameAs(expected);
        verify(merchantRepository)
                .findByOwnerUserId(ownerUserId, 0, 20);
        verifyNoMoreInteractions(merchantRepository);
    }

    @Test
    void shouldReturnEmptyPageWhenUserOwnsNoMerchants() {
        UUID ownerUserId = UUID.randomUUID();
        MerchantPage expected = new MerchantPage(
                List.of(), 0, 20, 0, 0
        );

        when(merchantRepository
                .findByOwnerUserId(ownerUserId, 0, 20))
                .thenReturn(expected);

        MerchantPage result = service.list(
                new ListMerchantsCommand(ownerUserId, 0, 20)
        );

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    void shouldRejectNullAuthenticatedUserId() {
        assertThatThrownBy(() -> service
                .list(new ListMerchantsCommand(null, 0, 20)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("authenticatedUserId must not be null");

        verifyNoInteractions(merchantRepository);
    }

    @Test
    void shouldRejectNullCommand() {
        assertThatThrownBy(() -> service
                .list(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");

        verifyNoInteractions(merchantRepository);
    }

    @Test
    void shouldRejectNegativePage() {
        UUID ownerUserId = UUID.randomUUID();
        assertThatThrownBy(() -> service
                .list(new ListMerchantsCommand(ownerUserId, -1, 20)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("page must not be negative");

        verifyNoInteractions(merchantRepository);
    }

    @Test
    void shouldRejectNonPositiveSize() {
        UUID ownerUserId = UUID.randomUUID();
        assertThatThrownBy(() -> service
                .list(new ListMerchantsCommand(ownerUserId, 0, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size must be greater than zero");

        verifyNoInteractions(merchantRepository);
    }
}