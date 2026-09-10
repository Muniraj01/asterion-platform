package com.asterion.merchant.infrastructure.persistence;

import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.in.CreateMerchantUseCase;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import com.asterion.merchant.application.port.out.MerchantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
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

    @Test
    void shouldPersistAndLoadOutboxEvent() {
        UUID eventId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

        MerchantOutboxEvent event = new MerchantOutboxEvent(
                eventId,
                merchantId,
                "MerchantCreated",
                "{\"merchantId\":\"" + merchantId + "\"}",
                createdAt,
                "NEW"
        );
        MerchantOutboxEvent saved = merchantOutboxRepository.save(event);

        assertThat(saved.eventId()).isEqualTo(eventId);
        assertThat(saved.aggregateId()).isEqualTo(merchantId);
        assertThat(saved.eventType()).isEqualTo("MerchantCreated");
        assertThat(saved.payload()).contains(merchantId.toString());
        assertThat(saved.createdAt()).isEqualTo(createdAt);
        assertThat(saved.status()).isEqualTo("NEW");
    }
}