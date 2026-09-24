package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.ActivateMerchantCommand;
import com.asterion.merchant.application.event.MerchantActivatedEvent;
import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import com.asterion.merchant.domain.model.MerchantStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivateMerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private MerchantOutboxRepository merchantOutboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    private ActivateMerchantService service;

    @BeforeEach
    void setUp() {
        service = new ActivateMerchantService(
                merchantRepository,
                merchantOutboxRepository,
                objectMapper
        );
    }

    @Test
    void shouldActivatePendingMerchant() throws Exception {
        Merchant merchant = createPendingMerchant();

        when(merchantRepository.findById(merchant.id()))
                .thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(any(MerchantActivatedEvent.class)))
                .thenReturn("{}");

        when(merchantOutboxRepository.save(any(MerchantOutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Merchant result = service.activate(
                new ActivateMerchantCommand(merchant.id())
        );

        assertThat(result.id()).isEqualTo(merchant.id());
        assertThat(result.status()).isEqualTo(MerchantStatus.ACTIVE);

        verify(merchantRepository).findById(merchant.id());
        verify(merchantRepository).save(merchant);
    }

    @Test
    void shouldSaveMerchantActivatedEventToOutbox() throws Exception {
        Merchant merchant = createPendingMerchant();

        when(merchantRepository.findById(merchant.id()))
                .thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(any(MerchantActivatedEvent.class)))
                .thenReturn("{\"merchantId\":\"" + merchant.id() + "\"}");

        when(merchantOutboxRepository.save(any(MerchantOutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Merchant result = service.activate(new ActivateMerchantCommand(merchant.id()));
        assertThat(result.status()).isEqualTo(MerchantStatus.ACTIVE);

        ArgumentCaptor<MerchantOutboxEvent> captor =
                ArgumentCaptor.forClass(MerchantOutboxEvent.class);
        verify(merchantOutboxRepository).save(captor.capture());

        MerchantOutboxEvent outboxEvent = captor.getValue();

        assertThat(outboxEvent.eventId()).isNotNull();
        assertThat(outboxEvent.aggregateId()).isEqualTo(merchant.id());
        assertThat(outboxEvent.eventType()).isEqualTo("merchant.activated.v1");
        assertThat(outboxEvent.payload()).contains(merchant.id().toString());
        assertThat(outboxEvent.createdAt()).isNotNull();
        assertThat(outboxEvent.status()).isEqualTo("NEW");
        assertThat(outboxEvent.claimedAt()).isNull();
    }

    @Test
    void shouldPublishEventWithCorrectMerchantDetails() throws Exception {
        Merchant merchant = createPendingMerchant();

        when(merchantRepository.findById(merchant.id()))
                .thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(any(MerchantActivatedEvent.class)))
                .thenReturn("{}");

        when(merchantOutboxRepository.save(any(MerchantOutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.activate(new ActivateMerchantCommand(merchant.id()));

        ArgumentCaptor<MerchantActivatedEvent> eventCaptor =
                ArgumentCaptor.forClass(MerchantActivatedEvent.class);
        verify(objectMapper).writeValueAsString(eventCaptor.capture());

        MerchantActivatedEvent event = eventCaptor.getValue();

        assertThat(event.eventId()).isNotNull();
        assertThat(event.merchantId()).isEqualTo(merchant.id());
        assertThat(event.ownerUserId()).isEqualTo(merchant.ownerUserId());
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void shouldRejectMissingMerchant() {
        UUID merchantId = UUID.randomUUID();

        when(merchantRepository.findById(merchantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service
                .activate(new ActivateMerchantCommand(merchantId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Merchant not found: " + merchantId);

        verify(merchantRepository).findById(merchantId);
        verify(merchantRepository, never()).save(any(Merchant.class));
        verifyNoInteractions(merchantOutboxRepository);
    }

    @Test
    void shouldRejectNullCommand() {
        assertThatThrownBy(() -> service
                .activate(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");

        verifyNoInteractions(merchantRepository);
        verifyNoInteractions(merchantOutboxRepository);
    }

    @Test
    void shouldRejectNullMerchantId() {
        ActivateMerchantCommand command = new ActivateMerchantCommand(null);
        assertThatThrownBy(() -> service
                .activate(command))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("merchantId must not be null");

        verifyNoInteractions(merchantRepository);
        verifyNoInteractions(merchantOutboxRepository);
    }

    @Test
    void shouldNotActivateAlreadyActiveMerchant() throws JsonProcessingException {
        Merchant merchant = createPendingMerchant();
        merchant.activate();

        when(merchantRepository.findById(merchant.id()))
                .thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> service
                .activate(new ActivateMerchantCommand(merchant.id())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only a pending merchant can be activated");

        verify(merchantRepository).findById(merchant.id());
        verify(merchantRepository, never()).save(any(Merchant.class));
        verifyNoInteractions(merchantOutboxRepository);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void shouldNotCreateOutboxEventWhenMerchantPersistenceFails() {
        Merchant merchant = createPendingMerchant();

        when(merchantRepository.findById(merchant.id()))
                .thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenThrow(new IllegalStateException("database failure"));

        assertThatThrownBy(() -> service
                .activate(new ActivateMerchantCommand(merchant.id())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database failure");

        verify(merchantRepository).save(merchant);
        verifyNoInteractions(merchantOutboxRepository);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void shouldPropagateSerializationFailure() throws Exception {
        Merchant merchant = createPendingMerchant();

        when(merchantRepository.findById(merchant.id()))
                .thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper
                .writeValueAsString(any(MerchantActivatedEvent.class)))
                .thenThrow(new JsonProcessingException("serialization failure") {});

        assertThatThrownBy(() -> service
                .activate(new ActivateMerchantCommand(merchant.id())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to serialize MerchantActivatedEvent");

        verify(merchantRepository).save(merchant);
        verifyNoInteractions(merchantOutboxRepository);
    }

    private Merchant createPendingMerchant() {
        return Merchant.create(
                UUID.randomUUID(),
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );
    }
}