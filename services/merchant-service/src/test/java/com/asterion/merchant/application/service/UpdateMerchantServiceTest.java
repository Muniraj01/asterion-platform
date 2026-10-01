package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.UpdateMerchantCommand;
import com.asterion.merchant.application.exception.MerchantOwnershipException;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import com.asterion.merchant.domain.model.MerchantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateMerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    private UpdateMerchantService service;

    @BeforeEach
    void setUp() {
        service = new UpdateMerchantService(merchantRepository);
    }

    @Test
    void shouldUpdateMerchantOwnedByAuthenticatedUser() {
        UUID ownerUserId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        Merchant merchant = Merchant.reconstitute(
                merchantId,
                ownerUserId,
                "Old Business",
                "Old Legal",
                "old@example.com",
                MerchantStatus.ACTIVE,
                Instant.parse("2026-01-01T10:00:00Z")
        );

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(merchantRepository.save(merchant)).thenReturn(merchant);

        Merchant result = service.update(
                new UpdateMerchantCommand(
                        merchantId,
                        ownerUserId,
                        "New Business",
                        "New Legal",
                        "new@example.com"
                )
        );

        assertThat(result).isSameAs(merchant);
        assertThat(result.businessName()).isEqualTo("New Business");
        assertThat(result.legalName()).isEqualTo("New Legal");
        assertThat(result.contactEmail()).isEqualTo("new@example.com");

        verify(merchantRepository).findById(merchantId);
        verify(merchantRepository).save(merchant);
        verifyNoMoreInteractions(merchantRepository);
    }

    @Test
    void shouldRejectMissingMerchant() {
        UUID merchantId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                new UpdateMerchantCommand(
                        merchantId,
                        ownerUserId,
                        "New Business",
                        "New Legal",
                        "new@example.com"
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Merchant not found: " + merchantId);

        verify(merchantRepository).findById(merchantId);
        verifyNoMoreInteractions(merchantRepository);
    }

    @Test
    void shouldRejectNonOwner() {
        UUID merchantId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        Merchant merchant = Merchant.create(
                ownerUserId,
                "Old Business",
                "Old Legal",
                "old@example.com"
        );

        when(merchantRepository.findById(merchantId))
                .thenReturn(Optional.of(
                        Merchant.reconstitute(
                                merchantId,
                                merchant.ownerUserId(),
                                merchant.businessName(),
                                merchant.legalName(),
                                merchant.contactEmail(),
                                merchant.status(),
                                merchant.createdAt()
                        )
                ));

        assertThatThrownBy(() -> service.update(
                new UpdateMerchantCommand(
                        merchantId,
                        otherUserId,
                        "New Business",
                        "New Legal",
                        "new@example.com"
                )
        ))
                .isInstanceOf(MerchantOwnershipException.class);

        verify(merchantRepository).findById(merchantId);
        verifyNoMoreInteractions(merchantRepository);
    }

    @Test
    void shouldRejectTerminatedMerchant() {
        UUID ownerUserId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        Merchant merchant = Merchant.create(
                ownerUserId,
                "Old Business",
                "Old Legal",
                "old@example.com"
        );

        merchant.activate();
        merchant.terminate();

        when(merchantRepository.findById(merchantId))
                .thenReturn(Optional.of(
                        Merchant.reconstitute(
                                merchantId,
                                ownerUserId,
                                merchant.businessName(),
                                merchant.legalName(),
                                merchant.contactEmail(),
                                merchant.status(),
                                merchant.createdAt()
                        )
                ));

        assertThatThrownBy(() -> service.update(
                new UpdateMerchantCommand(
                        merchantId,
                        ownerUserId,
                        "New Business",
                        "New Legal",
                        "new@example.com"
                )
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A terminated merchant cannot be updated");

        verify(merchantRepository).findById(merchantId);
        verifyNoMoreInteractions(merchantRepository);
    }

    @Test
    void shouldRejectNullCommand() {
        assertThatThrownBy(() -> service.update(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");

        verifyNoInteractions(merchantRepository);
    }

    @Test
    void shouldRejectNullMerchantId() {
        UUID ownerUserId = UUID.randomUUID();

        assertThatThrownBy(() -> service.update(
                new UpdateMerchantCommand(
                        null,
                        ownerUserId,
                        "New Business",
                        "New Legal",
                        "new@example.com"
                )
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("merchantId must not be null");

        verifyNoInteractions(merchantRepository);
    }

    @Test
    void shouldRejectNullAuthenticatedUserId() {
        UUID merchantId = UUID.randomUUID();

        assertThatThrownBy(() -> service.update(
                new UpdateMerchantCommand(
                        merchantId,
                        null,
                        "New Business",
                        "New Legal",
                        "new@example.com"
                )
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("authenticatedUserId must not be null");

        verifyNoInteractions(merchantRepository);
    }
}