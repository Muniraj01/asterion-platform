package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.SuspendMerchantCommand;
import com.asterion.merchant.application.event.MerchantSuspendedEvent;
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
class SuspendMerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private MerchantOutboxRepository merchantOutboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    private SuspendMerchantService service;

    @BeforeEach
    void setUp() {
        service = new SuspendMerchantService(
                merchantRepository,
                merchantOutboxRepository,
                objectMapper
        );
    }

    @Test
    void shouldSuspendActiveMerchant() throws Exception {
        Merchant merchant = createActiveMerchant();

        when(merchantRepository.findById(merchant.id()))
                .thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(objectMapper.writeValueAsString(
                any(MerchantSuspendedEvent.class)))
                .thenReturn("{}");

        Merchant result = service.suspend(
                new SuspendMerchantCommand(merchant.id(), merchant.ownerUserId())
        );

        assertThat(result).isSameAs(merchant);
        assertThat(result.status()).isEqualTo(MerchantStatus.SUSPENDED);

        verify(merchantRepository).findById(merchant.id());
        verify(merchantRepository).save(merchant);
        verify(merchantOutboxRepository).save(any(MerchantOutboxEvent.class));
    }

    @Test
    void shouldSaveSuspendedEventToOutbox() throws Exception {
        Merchant merchant = createActiveMerchant();

        when(merchantRepository.findById(merchant.id()))
                .thenReturn(Optional.of(merchant));

        when(merchantRepository.save(any(Merchant.class)))
                .thenReturn(merchant);

        when(objectMapper.writeValueAsString(
                any(MerchantSuspendedEvent.class)))
                .thenReturn("{\"merchantId\":\"" + merchant.id() + "\"}");

        ArgumentCaptor<MerchantOutboxEvent> captor =
                ArgumentCaptor.forClass(MerchantOutboxEvent.class);

        service.suspend(
                new SuspendMerchantCommand(merchant.id(), merchant.ownerUserId())
        );

        verify(merchantOutboxRepository).save(captor.capture());

        MerchantOutboxEvent event = captor.getValue();

        assertThat(event.eventId()).isNotNull();
        assertThat(event.aggregateId()).isEqualTo(merchant.id());
        assertThat(event.eventType()).isEqualTo("merchant.suspended.v1");
        assertThat(event.payload()).contains(merchant.id().toString());
        assertThat(event.status()).isEqualTo("NEW");
        assertThat(event.claimedAt()).isNull();
    }

    @Test
    void shouldRejectNonOwner() {
        Merchant merchant = createActiveMerchant();
        UUID otherUserId = UUID.randomUUID();

        when(merchantRepository.findById(merchant.id()))
                .thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> service.suspend(
                new SuspendMerchantCommand(merchant.id(), otherUserId)
                )
        )
                .isInstanceOf(MerchantOwnershipException.class);

        assertThat(merchant.status()).isEqualTo(MerchantStatus.ACTIVE);
        verify(merchantRepository, never()).save(any());
        verifyNoInteractions(merchantOutboxRepository);
    }

    @Test
    void shouldRejectMissingMerchant() {
        UUID merchantId = UUID.randomUUID();

        when(merchantRepository.findById(merchantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.suspend(
                new SuspendMerchantCommand(merchantId, UUID.randomUUID())
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Merchant not found: " + merchantId);

        verifyNoInteractions(merchantOutboxRepository);
    }

    @Test
    void shouldRejectAlreadySuspendedMerchant() {
        Merchant merchant = createActiveMerchant();
        merchant.suspend();

        when(merchantRepository.findById(merchant.id()))
                .thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> service.suspend(
                new SuspendMerchantCommand(merchant.id(), merchant.ownerUserId())
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only an active merchant can be suspended");

        verify(merchantRepository, never()).save(any());
        verifyNoInteractions(merchantOutboxRepository);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void shouldRejectNullCommand() {
        assertThatThrownBy(() -> service.suspend(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");

        verifyNoInteractions(merchantRepository);
    }

    @Test
    void shouldRejectNullMerchantId() {
        assertThatThrownBy(() -> service.suspend(
                new SuspendMerchantCommand(null, UUID.randomUUID())
                )
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage("merchantId must not be null");

        verifyNoInteractions(merchantRepository);
    }

    @Test
    void shouldRejectNullAuthenticatedUserId() {
        assertThatThrownBy(() -> service.suspend(
                new SuspendMerchantCommand(UUID.randomUUID(), null))
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage("authenticatedUserId must not be null");

        verifyNoInteractions(merchantRepository);
    }

    @Test
    void shouldPropagateSerializationFailure() throws Exception {
        Merchant merchant = createActiveMerchant();

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));
        when(merchantRepository.save(any(Merchant.class))).thenReturn(merchant);
        when(objectMapper.writeValueAsString(any(MerchantSuspendedEvent.class)))
                .thenThrow(new JsonProcessingException("serialization failure") {});

        assertThatThrownBy(() -> service.suspend(
                new SuspendMerchantCommand(merchant.id(), merchant.ownerUserId())
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to serialize MerchantSuspendedEvent");

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