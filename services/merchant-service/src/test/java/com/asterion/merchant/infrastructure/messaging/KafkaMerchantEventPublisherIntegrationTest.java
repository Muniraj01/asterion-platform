package com.asterion.merchant.infrastructure.messaging;

import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import com.asterion.merchant.application.service.MerchantOutboxPublisher;
import com.asterion.merchant.infrastructure.persistence.MerchantOutboxJpaEntity;
import com.asterion.merchant.infrastructure.persistence.MerchantOutboxJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class KafkaMerchantEventPublisherIntegrationTest {

    private static final String TOPIC = "merchant.events";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            "postgres:17-alpine")
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

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("asterion.merchant.kafka.topic", () -> TOPIC);
    }

    @BeforeEach
    void cleanDatabase() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldPublishOutboxEventToKafkaAndMarkItPublished() throws JsonProcessingException {
        UUID eventId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        MerchantOutboxEvent event = new MerchantOutboxEvent(
                eventId,
                merchantId,
                "merchant.created.v1",
                "{\"merchantId\":\"" + merchantId + "\"}",
                createdAt,
                "NEW"
        );
        merchantOutboxRepository.save(event);

        MerchantOutboxPublisher publisher = new MerchantOutboxPublisher(
                merchantOutboxRepository, eventPublisher);
        publisher.publishPending(10);

        MerchantOutboxJpaEntity persisted = jpaRepository.findById(eventId).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo("PUBLISHED");
        assertThat(persisted.getPublishedAt()).isNotNull();
        ConsumerRecord<String, String> record = consumeSingleRecord();
        assertThat(record.topic()).isEqualTo(TOPIC);
        assertThat(record.key()).isEqualTo(merchantId.toString());
        assertThat(objectMapper.readTree(record.value()))
                .isEqualTo(objectMapper.readTree(event.payload()));
        assertThat(headerValue(record, "eventId")).isEqualTo(eventId.toString());
        assertThat(headerValue(record, "eventType"))
                .isEqualTo("merchant.created.v1");
        assertThat(headerValue(record, "aggregateId"))
                .isEqualTo(merchantId.toString());
    }

    private ConsumerRecord<String, String> consumeSingleRecord() {
        Properties properties = new Properties();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers());

        properties.put(ConsumerConfig.GROUP_ID_CONFIG,
                "merchant-service-test-" + UUID.randomUUID());

        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());

        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());

        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                OffsetResetStrategy.EARLIEST.name().toLowerCase());

        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(Collections.singletonList(TOPIC));
            long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
            while (System.nanoTime() < deadline) {
                var records = consumer.poll(Duration.ofMillis(250));
                if (!records.isEmpty()) {
                    return records.iterator().next();
                }
            }
        }
        throw new AssertionError("No Kafka record received within 15 seconds");
    }

    private String headerValue(ConsumerRecord<String, String> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        assertThat(header)
                .as("Kafka header '%s'", headerName)
                .isNotNull();
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}