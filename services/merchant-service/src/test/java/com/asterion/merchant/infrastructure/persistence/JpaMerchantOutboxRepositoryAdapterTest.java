package com.asterion.merchant.infrastructure.persistence;

import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class JpaMerchantOutboxRepositoryAdapterTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("merchant_db")
                    .withUsername("merchant")
                    .withPassword("merchant");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MerchantOutboxRepository merchantOutboxRepository;

    @Autowired
    private MerchantOutboxJpaRepository jpaRepository;

    @BeforeEach
    void cleanDatabase() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldPersistAndLoadOutboxEvent() {
        UUID eventId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

        MerchantOutboxEvent event = event(eventId, merchantId, createdAt, "NEW");
        MerchantOutboxEvent saved = merchantOutboxRepository.save(event);

        assertThat(saved.eventId()).isEqualTo(eventId);
        assertThat(saved.aggregateId()).isEqualTo(merchantId);
        assertThat(saved.eventType()).isEqualTo("merchant.created.v1");
        assertThat(saved.payload()).contains(merchantId.toString());
        assertThat(saved.createdAt()).isEqualTo(createdAt);
        assertThat(saved.status()).isEqualTo("NEW");
    }

    @Test
    void shouldFindOnlyNewPendingEvents() {
        MerchantOutboxEvent pending = event(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now().minusSeconds(10),
                "NEW"
        );

        MerchantOutboxEvent published = event(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                "PUBLISHED"
        );

        merchantOutboxRepository.save(pending);
        merchantOutboxRepository.save(published);
        List<MerchantOutboxEvent> result = merchantOutboxRepository.findPending(10);

        assertThat(result)
                .extracting(MerchantOutboxEvent::eventId)
                .containsExactly(pending.eventId());
    }

    @Test
    void shouldReturnPendingEventsInCreatedAtOrder() {
        Instant firstCreatedAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant secondCreatedAt = Instant.parse("2026-01-01T10:00:01Z");
        Instant thirdCreatedAt = Instant.parse("2026-01-01T10:00:02Z");

        MerchantOutboxEvent first = event(
                UUID.randomUUID(),
                UUID.randomUUID(),
                firstCreatedAt,
                "NEW"
        );

        MerchantOutboxEvent second = event(
                UUID.randomUUID(),
                UUID.randomUUID(),
                secondCreatedAt,
                "NEW"
        );

        MerchantOutboxEvent third = event(
                UUID.randomUUID(),
                UUID.randomUUID(),
                thirdCreatedAt,
                "NEW"
        );

        // Deliberately save out of chronological order.
        merchantOutboxRepository.save(third);
        merchantOutboxRepository.save(first);
        merchantOutboxRepository.save(second);

        List<MerchantOutboxEvent> result = merchantOutboxRepository.findPending(10);
        assertThat(result)
                .extracting(MerchantOutboxEvent::eventId)
                .containsExactly(first.eventId(), second.eventId(), third.eventId());
    }

    @Test
    void shouldRespectPendingEventLimit() {
        MerchantOutboxEvent first = event(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-01-01T10:00:00Z"),
                "NEW"
        );

        MerchantOutboxEvent second = event(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-01-01T10:00:01Z"),
                "NEW"
        );

        MerchantOutboxEvent third = event(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-01-01T10:00:02Z"),
                "NEW"
        );

        merchantOutboxRepository.save(first);
        merchantOutboxRepository.save(second);
        merchantOutboxRepository.save(third);

        List<MerchantOutboxEvent> result = merchantOutboxRepository.findPending(2);
        assertThat(result)
                .extracting(MerchantOutboxEvent::eventId)
                .containsExactly(first.eventId(), second.eventId());
    }

    @Test
    void shouldMarkEventPublishedAndSetPublishedAt() {
        MerchantOutboxEvent event = event(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now().truncatedTo(ChronoUnit.MICROS),
                "NEW"
        );
        merchantOutboxRepository.save(event);

        Instant beforePublish = Instant.now();
        merchantOutboxRepository.markPublished(event.eventId());
        Instant afterPublish = Instant.now();

        MerchantOutboxJpaEntity persisted =
                jpaRepository.findById(event.eventId())
                        .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo("PUBLISHED");

        assertThat(persisted.getPublishedAt())
                .isNotNull()
                .isBetween(beforePublish, afterPublish);
    }

    @Test
    void shouldRejectNonPositivePendingLimit() {
        assertThatThrownBy(() -> merchantOutboxRepository
                .findPending(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be greater than zero");

        assertThatThrownBy(() -> merchantOutboxRepository.
                findPending(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be greater than zero");
    }

    @Test
    void shouldFailWhenMarkingUnknownEventPublished() {
        UUID eventId = UUID.randomUUID();
        assertThatThrownBy(() -> merchantOutboxRepository
                .markPublished(eventId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Outbox event not found: " + eventId);
    }

    private MerchantOutboxEvent event(UUID eventId, UUID merchantId,
                                      Instant createdAt, String status) {
        return new MerchantOutboxEvent(
                eventId,
                merchantId,
                "merchant.created.v1",
                "{\"merchantId\":\"" + merchantId + "\"}",
                createdAt,
                status
        );
    }
}
