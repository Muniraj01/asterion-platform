package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.CreateMerchantCommand;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import com.asterion.merchant.domain.model.MerchantStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateMerchantServiceTest {

    private final MerchantRepository merchantRepository =
            mock(MerchantRepository.class);

    private final CreateMerchantService service =
            new CreateMerchantService(merchantRepository);

    @Test
    void shouldCreateMerchantInPendingStatus() {
        UUID ownerUserId = UUID.randomUUID();
        CreateMerchantCommand command = new CreateMerchantCommand(
                ownerUserId,
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );

        when(merchantRepository.save(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Merchant result = service.create(command);

        assertThat(result.id()).isNotNull();
        assertThat(result.ownerUserId()).isEqualTo(ownerUserId);
        assertThat(result.businessName()).isEqualTo("Asterion Technologies");
        assertThat(result.legalName())
                .isEqualTo("Asterion Technologies Private Limited");
        assertThat(result.contactEmail())
                .isEqualTo("merchant@example.com");
        assertThat(result.status()).isEqualTo(MerchantStatus.PENDING);
        assertThat(result.createdAt()).isNotNull();
        verify(merchantRepository).save(result);
    }

    @Test
    void shouldPersistOnlyTheCreatedMerchant() {
        UUID ownerUserId = UUID.randomUUID();
        CreateMerchantCommand command = new CreateMerchantCommand(
                ownerUserId,
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );
        service.create(command);

        verify(merchantRepository, times(1)).save(any(Merchant.class));
        verifyNoMoreInteractions(merchantRepository);
    }
}