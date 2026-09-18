package com.asterion.merchant.infrastructure.scheduling;

import com.asterion.merchant.application.service.MerchantOutboxPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MerchantOutboxSchedulerTest {

    private MerchantOutboxPublisher merchantOutboxPublisher;
    private MerchantOutboxScheduler scheduler;

    @BeforeEach
    void setUp() {
        merchantOutboxPublisher = mock(MerchantOutboxPublisher.class);
        scheduler = new MerchantOutboxScheduler(merchantOutboxPublisher, 100);
    }

    @Test
    void shouldPublishPendingEventsUsingConfiguredBatchSize() {
        scheduler.publishPendingEvents();
        verify(merchantOutboxPublisher).publishPending(100);
    }

    @Test
    void shouldRejectNonPositiveBatchSize() {
        assertThatThrownBy(
                () -> new MerchantOutboxScheduler(merchantOutboxPublisher, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("batchSize must be greater than zero");

        assertThatThrownBy(
                () -> new MerchantOutboxScheduler(merchantOutboxPublisher, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("batchSize must be greater than zero");
    }

    @Test
    void shouldNotPublishWhenSchedulerHasNotBeenTriggered() {
        verifyNoInteractions(merchantOutboxPublisher);
    }
}