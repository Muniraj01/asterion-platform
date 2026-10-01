package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.ReactivateMerchantCommand;
import com.asterion.merchant.application.event.MerchantReactivatedEvent;
import com.asterion.merchant.application.exception.MerchantOwnershipException;
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
class ReactivateMerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private MerchantOutboxRepository merchantOutboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    private ReactivateMerchantService service;

    @BeforeEach
    void setUp() {
        service = new ReactivateMerchantService(merchantRepository,
                merchantOutboxRepository, objectMapper);
    }

    @Test
    void shouldReactivateSuspendedMerchant() throws Exception {
        Merchant merchant = createSuspendedMerchant();

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(any(MerchantReactivatedEvent.class)))
                .thenReturn("{}");

        when(merchantOutboxRepository.save(any(MerchantOutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Merchant result = service.reactivate(
                new ReactivateMerchantCommand(merchant.id(), merchant.ownerUserId())
        );

        assertThat(result).isSameAs(merchant);
        assertThat(result.status()).isEqualTo(MerchantStatus.ACTIVE);

        verify(merchantRepository).findById(merchant.id());
        verify(merchantRepository).save(merchant);
        verify(merchantOutboxRepository).save(any(MerchantOutboxEvent.class));
    }

    @Test
    void shouldSaveMerchantReactivatedEventToOutbox() throws Exception {
        Merchant merchant = createSuspendedMerchant();

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(
                any(MerchantReactivatedEvent.class)))
                .thenReturn("{\"merchantId\":\"" + merchant.id() + "\"}");

        when(merchantOutboxRepository.save(any(MerchantOutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Merchant result = service.reactivate(
                new ReactivateMerchantCommand(merchant.id(), merchant.ownerUserId())
        );

        assertThat(result.status()).isEqualTo(MerchantStatus.ACTIVE);
        ArgumentCaptor<MerchantOutboxEvent> captor =
                ArgumentCaptor.forClass(MerchantOutboxEvent.class);

        verify(merchantOutboxRepository).save(captor.capture());
        MerchantOutboxEvent outboxEvent = captor.getValue();

        assertThat(outboxEvent.eventId()).isNotNull();
        assertThat(outboxEvent.aggregateId()).isEqualTo(merchant.id());
        assertThat(outboxEvent.eventType()).isEqualTo("merchant.reactivated.v1");
        assertThat(outboxEvent.payload()).contains(merchant.id().toString());
        assertThat(outboxEvent.createdAt()).isNotNull();
        assertThat(outboxEvent.status()).isEqualTo("NEW");
        assertThat(outboxEvent.claimedAt()).isNull();
    }

    @Test
    void shouldPublishEventWithCorrectMerchantDetails() throws Exception {
        Merchant merchant = createSuspendedMerchant();

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(any(MerchantReactivatedEvent.class)))
                .thenReturn("{}");

        when(merchantOutboxRepository.save(any(MerchantOutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.reactivate(
                new ReactivateMerchantCommand(merchant.id(), merchant.ownerUserId())
        );

        ArgumentCaptor<MerchantReactivatedEvent> eventCaptor =
                ArgumentCaptor.forClass(MerchantReactivatedEvent.class);
        verify(objectMapper).writeValueAsString(eventCaptor.capture());
        MerchantReactivatedEvent event = eventCaptor.getValue();

        assertThat(event.eventId()).isNotNull();
        assertThat(event.merchantId()).isEqualTo(merchant.id());
        assertThat(event.ownerUserId()).isEqualTo(merchant.ownerUserId());
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void shouldRejectMissingMerchant() {
        UUID merchantId = UUID.randomUUID();
        UUID authenticatedUserId = UUID.randomUUID();

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service
                .reactivate(new ReactivateMerchantCommand(merchantId, authenticatedUserId))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Merchant not found: " + merchantId);

        verify(merchantRepository).findById(merchantId);
        verify(merchantRepository, never()).save(any(Merchant.class));
        verifyNoInteractions(merchantOutboxRepository);
    }

    @Test
    void shouldRejectNonOwner() {
        Merchant merchant = createSuspendedMerchant();
        UUID authenticatedUserId = UUID.randomUUID();

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> service
                .reactivate(new ReactivateMerchantCommand(merchant.id(), authenticatedUserId))
        )
                .isInstanceOf(MerchantOwnershipException.class)
                .hasMessage("Authenticated user does not own merchant: " + merchant.id());

        assertThat(merchant.status()).isEqualTo(MerchantStatus.SUSPENDED);

        verify(merchantRepository).findById(merchant.id());
        verify(merchantRepository, never()).save(any(Merchant.class));
        verifyNoInteractions(merchantOutboxRepository);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void shouldNotReactivateAlreadyActiveMerchant() throws JsonProcessingException {
        Merchant merchant = createSuspendedMerchant();
        merchant.reactivate();

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> service
                .reactivate(new ReactivateMerchantCommand(
                        merchant.id(), merchant.ownerUserId()))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only a suspended merchant can be reactivated");

        verify(merchantRepository).findById(merchant.id());
        verify(merchantRepository, never()).save(any(Merchant.class));
        verifyNoInteractions(merchantOutboxRepository);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void shouldRejectNullCommand() {
        assertThatThrownBy(() -> service.reactivate(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");

        verifyNoInteractions(merchantRepository);
        verifyNoInteractions(merchantOutboxRepository);
    }

    @Test
    void shouldRejectNullMerchantId() {
        ReactivateMerchantCommand command =
                new ReactivateMerchantCommand(null, null);

        assertThatThrownBy(() -> service.reactivate(command))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("merchantId must not be null");

        verifyNoInteractions(merchantRepository);
        verifyNoInteractions(merchantOutboxRepository);
    }

    @Test
    void shouldRejectNullAuthenticatedUserId() {
        ReactivateMerchantCommand command =
                new ReactivateMerchantCommand(UUID.randomUUID(), null);

        assertThatThrownBy(() -> service.reactivate(command))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("authenticatedUserId must not be null");

        verifyNoInteractions(merchantRepository);
        verifyNoInteractions(merchantOutboxRepository);
    }

    @Test
    void shouldNotCreateOutboxEventWhenMerchantPersistenceFails() {
        Merchant merchant = createSuspendedMerchant();

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenThrow(new IllegalStateException("database failure"));

        assertThatThrownBy(() -> service.reactivate(
                new ReactivateMerchantCommand(merchant.id(), merchant.ownerUserId()))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database failure");

        verify(merchantRepository).save(merchant);
        verifyNoInteractions(merchantOutboxRepository);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void shouldPropagateSerializationFailure() throws Exception {
        Merchant merchant = createSuspendedMerchant();

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(any(MerchantReactivatedEvent.class)))
                .thenThrow(new JsonProcessingException("serialization failure") {});

        assertThatThrownBy(() -> service.reactivate(
                new ReactivateMerchantCommand(merchant.id(), merchant.ownerUserId()))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to serialize MerchantReactivatedEvent");

        verify(merchantRepository).save(merchant);
        verifyNoInteractions(merchantOutboxRepository);
    }

    private Merchant createSuspendedMerchant() {
        Merchant merchant = Merchant.create(
                UUID.randomUUID(),
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );

        merchant.activate();
        merchant.suspend();

        return merchant;
    }
}