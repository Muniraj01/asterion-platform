package com.asterion.merchant.infrastructure.messaging;

import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import com.asterion.merchant.application.service.MerchantOutboxPublisher;
import com.asterion.merchant.infrastructure.persistence.MerchantOutboxJpaEntity;
import com.asterion.merchant.infrastructure.persistence.MerchantOutboxJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class KafkaMerchantEventPublisherFailureIntegrationTest {

    private static final String TOPIC = "merchant.events";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("asterion")
                    .withUsername("asterion")
                    .withPassword("asterion");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.8.0"));

    @Autowired
    private MerchantOutboxRepository merchantOutboxRepository;

    @Autowired
    private MerchantOutboxJpaRepository jpaRepository;

    @Autowired
    private KafkaMerchantEventPublisher eventPublisher;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("asterion.merchant.kafka.topic", () -> TOPIC);
        /*
         * Make broker failure deterministic and reasonably fast.
         */
        registry.add("spring.kafka.producer.retries", () -> "0");
        registry.add("spring.kafka.producer.properties.max.block.ms", () -> "3000");
        registry.add("spring.kafka.producer.properties.request.timeout.ms", () -> "2000");
        registry.add("spring.kafka.producer.properties.delivery.timeout.ms", () -> "3000");
    }

    @BeforeEach
    void cleanDatabase() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldKeepOutboxEventProcessingWhenKafkaPublicationFails() {
        UUID eventId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

        MerchantOutboxEvent event = new MerchantOutboxEvent(
                eventId,
                merchantId,
                "merchant.created.v1",
                "{\"merchantId\":\"" + merchantId + "\"}",
                createdAt,
                "NEW",
                null
        );
        merchantOutboxRepository.save(event);

        /*
         * The outbox transaction has already committed.
         *
         * Kafka now becomes unavailable before the publisher
         * attempts to publish the event.
         */
        KAFKA.stop();

        MerchantOutboxPublisher publisher = new MerchantOutboxPublisher(
                merchantOutboxRepository, eventPublisher);

        assertThatThrownBy(
                () -> publisher.publishPending(10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to publish merchant outbox event");

        MerchantOutboxJpaEntity persisted = jpaRepository.findById(eventId).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo("PROCESSING");
        assertThat(persisted.getClaimedAt()).isNotNull();
        assertThat(persisted.getPublishedAt()).isNull();
    }
}
