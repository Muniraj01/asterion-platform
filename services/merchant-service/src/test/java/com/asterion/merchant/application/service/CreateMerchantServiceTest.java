package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.CreateMerchantCommand;
import com.asterion.merchant.application.port.in.CreateMerchantUseCase;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import com.asterion.merchant.domain.model.MerchantStatus;
import com.asterion.merchant.application.event.MerchantCreatedEvent;
import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateMerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private MerchantOutboxRepository merchantOutboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    private CreateMerchantService service;

    @BeforeEach
    void setUp() {
        service = new CreateMerchantService(merchantRepository,
                merchantOutboxRepository, objectMapper);
    }

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
        verify(merchantRepository).save(any(Merchant.class));
    }

    @Test
    void shouldPersistOnlyTheCreatedMerchant() throws JsonProcessingException {
        when(merchantRepository.save(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any(MerchantCreatedEvent.class)))
                .thenReturn("{}");
        when(merchantOutboxRepository.save(any(MerchantOutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UUID ownerUserId = UUID.randomUUID();
        CreateMerchantCommand command = new CreateMerchantCommand(
                ownerUserId,
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );
        service.create(command);

        verify(merchantRepository, times(1))
                .save(any(Merchant.class));
        verifyNoMoreInteractions(merchantRepository);
    }

    @Test
    void shouldSaveMerchantCreatedEventToOutbox() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        CreateMerchantCommand command = new CreateMerchantCommand(
                ownerUserId,
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );

        Merchant merchant = Merchant.create(
                ownerUserId,
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );

        when(merchantRepository.save(any(Merchant.class)))
                .thenReturn(merchant);

        when(objectMapper.writeValueAsString(any(MerchantCreatedEvent.class)))
                .thenReturn("{\"merchantId\":\"" + merchant.id() + "\"}");

        when(merchantOutboxRepository.save(any(MerchantOutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateMerchantUseCase service = new CreateMerchantService(
                merchantRepository,
                merchantOutboxRepository,
                objectMapper
        );

        Merchant result = service.create(command);
        assertThat(result.id()).isEqualTo(merchant.id());

        ArgumentCaptor<MerchantOutboxEvent> captor =
                ArgumentCaptor.forClass(MerchantOutboxEvent.class);
        verify(merchantOutboxRepository).save(captor.capture());

        MerchantOutboxEvent outboxEvent = captor.getValue();
        assertThat(outboxEvent.eventId()).isNotNull();
        assertThat(outboxEvent.aggregateId())
                .isEqualTo(merchant.id());
        assertThat(outboxEvent.eventType())
                .isEqualTo("MerchantCreated");
        assertThat(outboxEvent.payload())
                .contains(merchant.id().toString());
        assertThat(outboxEvent.createdAt())
                .isEqualTo(merchant.createdAt());
        assertThat(outboxEvent.status())
                .isEqualTo("NEW");
    }
}