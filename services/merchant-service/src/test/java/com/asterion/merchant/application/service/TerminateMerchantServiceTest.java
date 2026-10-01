package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.TerminateMerchantCommand;
import com.asterion.merchant.application.event.MerchantTerminatedEvent;
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
class TerminateMerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private MerchantOutboxRepository merchantOutboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    private TerminateMerchantService service;

    @BeforeEach
    void setUp() {
        service = new TerminateMerchantService(merchantRepository,
                merchantOutboxRepository, objectMapper);
    }

    @Test
    void shouldTerminateActiveMerchant() throws Exception {
        Merchant merchant = createActiveMerchant();

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(
                any(MerchantTerminatedEvent.class))).thenReturn("{}");

        when(merchantOutboxRepository.save(any(MerchantOutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Merchant result = service.terminate(
                new TerminateMerchantCommand(merchant.id(), merchant.ownerUserId())
        );

        assertThat(result).isSameAs(merchant);
        assertThat(result.status()).isEqualTo(MerchantStatus.TERMINATED);

        verify(merchantRepository).findById(merchant.id());
        verify(merchantRepository).save(merchant);
        verify(merchantOutboxRepository).save(any(MerchantOutboxEvent.class));
    }

    @Test
    void shouldSaveMerchantTerminatedEventToOutbox() throws Exception {
        Merchant merchant = createActiveMerchant();

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(
                any(MerchantTerminatedEvent.class)))
                .thenReturn("{\"merchantId\":\"" + merchant.id() + "\"}");

        when(merchantOutboxRepository.save(any(MerchantOutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Merchant result = service.terminate(
                new TerminateMerchantCommand(merchant.id(), merchant.ownerUserId())
        );

        assertThat(result.status()).isEqualTo(MerchantStatus.TERMINATED);

        ArgumentCaptor<MerchantOutboxEvent> captor =
                ArgumentCaptor.forClass(MerchantOutboxEvent.class);

        verify(merchantOutboxRepository).save(captor.capture());

        MerchantOutboxEvent outboxEvent = captor.getValue();

        assertThat(outboxEvent.eventId()).isNotNull();
        assertThat(outboxEvent.aggregateId()).isEqualTo(merchant.id());
        assertThat(outboxEvent.eventType()).isEqualTo("merchant.terminated.v1");
        assertThat(outboxEvent.payload()).contains(merchant.id().toString());
        assertThat(outboxEvent.createdAt()).isNotNull();
        assertThat(outboxEvent.status()).isEqualTo("NEW");
        assertThat(outboxEvent.claimedAt()).isNull();
    }

    @Test
    void shouldPublishEventWithCorrectMerchantDetails() throws Exception {
        Merchant merchant = createActiveMerchant();

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(
                any(MerchantTerminatedEvent.class))
        )
                .thenReturn("{}");

        when(merchantOutboxRepository.save(any(MerchantOutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.terminate(
                new TerminateMerchantCommand(merchant.id(), merchant.ownerUserId())
        );

        ArgumentCaptor<MerchantTerminatedEvent> eventCaptor =
                ArgumentCaptor.forClass(MerchantTerminatedEvent.class);
        verify(objectMapper).writeValueAsString(eventCaptor.capture());
        MerchantTerminatedEvent event = eventCaptor.getValue();

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
                .terminate(new TerminateMerchantCommand(merchantId, authenticatedUserId))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Merchant not found: " + merchantId);

        verify(merchantRepository).findById(merchantId);
        verify(merchantRepository, never()).save(any(Merchant.class));
        verifyNoInteractions(merchantOutboxRepository);
    }

    @Test
    void shouldRejectNonOwner() {
        Merchant merchant = createActiveMerchant();
        UUID authenticatedUserId = UUID.randomUUID();

        when(merchantRepository.findById(merchant.id()))
                .thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> service
                .terminate(new TerminateMerchantCommand(merchant.id(), authenticatedUserId))
        )
                .isInstanceOf(MerchantOwnershipException.class)
                .hasMessage("Authenticated user does not own merchant: " + merchant.id());

        assertThat(merchant.status()).isEqualTo(MerchantStatus.ACTIVE);

        verify(merchantRepository).findById(merchant.id());
        verify(merchantRepository, never()).save(any(Merchant.class));
        verifyNoInteractions(merchantOutboxRepository);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void shouldNotTerminateNonActiveMerchant() {
        Merchant merchant = createActiveMerchant();
        merchant.suspend();

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> service
                .terminate(new TerminateMerchantCommand(merchant.id(), merchant.ownerUserId()))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only an active merchant can be terminated");

        assertThat(merchant.status()).isEqualTo(MerchantStatus.SUSPENDED);

        verify(merchantRepository).findById(merchant.id());
        verify(merchantRepository, never()).save(any(Merchant.class));
        verifyNoInteractions(merchantOutboxRepository);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void shouldNotTerminateAlreadyTerminatedMerchantAndShouldNotCreateOutboxEvent() {
        Merchant merchant = createActiveMerchant();
        merchant.terminate();

        assertThat(merchant.status()).isEqualTo(MerchantStatus.TERMINATED);

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> service
                .terminate(new TerminateMerchantCommand(merchant.id(), merchant.ownerUserId()))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only an active merchant can be terminated");

        assertThat(merchant.status()).isEqualTo(MerchantStatus.TERMINATED);

        verify(merchantRepository).findById(merchant.id());
        verify(merchantRepository, never()).save(any(Merchant.class));
        verifyNoInteractions(merchantOutboxRepository);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void shouldRejectNullCommand() {
        assertThatThrownBy(() -> service.terminate(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");

        verifyNoInteractions(merchantRepository);
        verifyNoInteractions(merchantOutboxRepository);
    }

    @Test
    void shouldRejectNullMerchantId() {
        TerminateMerchantCommand command =
                new TerminateMerchantCommand(null, null);

        assertThatThrownBy(() -> service.terminate(command))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("merchantId must not be null");

        verifyNoInteractions(merchantRepository);
        verifyNoInteractions(merchantOutboxRepository);
    }

    @Test
    void shouldRejectNullAuthenticatedUserId() {
        TerminateMerchantCommand command =
                new TerminateMerchantCommand(UUID.randomUUID(), null);

        assertThatThrownBy(() -> service.terminate(command))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("authenticatedUserId must not be null");

        verifyNoInteractions(merchantRepository);
        verifyNoInteractions(merchantOutboxRepository);
    }

    @Test
    void shouldNotCreateOutboxEventWhenMerchantPersistenceFails() {
        Merchant merchant = createActiveMerchant();

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenThrow(new IllegalStateException("database failure"));

        assertThatThrownBy(() -> service.terminate(
                new TerminateMerchantCommand(merchant.id(), merchant.ownerUserId()))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database failure");

        verify(merchantRepository).save(merchant);
        verifyNoInteractions(merchantOutboxRepository);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void shouldPropagateSerializationFailure() throws Exception {
        Merchant merchant = createActiveMerchant();

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(
                any(MerchantTerminatedEvent.class)))
                .thenThrow(new JsonProcessingException("serialization failure") {});

        assertThatThrownBy(() -> service.terminate(
                new TerminateMerchantCommand(merchant.id(), merchant.ownerUserId()))
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to serialize MerchantTerminatedEvent");

        verify(merchantRepository).save(merchant);
        verifyNoInteractions(merchantOutboxRepository);
    }

    private Merchant createActiveMerchant() {
        Merchant merchant = Merchant.create(
                UUID.randomUUID(),
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );

        merchant.activate();
        return merchant;
    }
}