package com.asterion.order.infrastructure.messaging;

import com.asterion.events.PaymentSucceededEvent;
import com.asterion.order.application.port.in.HandlePaymentOutcomeUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@Testcontainers
@SpringBootTest
class PaymentOutcomeKafkaReliabilityIntegrationTest {

    private static final String TOPIC = "payment.outcome.reliability.test";
    private static final String DLT = TOPIC + ".DLT";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("asterion_order")
                    .withUsername("asterion")
                    .withPassword("asterion");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.8.0"));

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HandlePaymentOutcomeUseCase useCase;

    @BeforeAll
    static void createKafkaTopics() throws Exception {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(properties)) {
            adminClient.createTopics(List.of(
                    new NewTopic(TOPIC, 1, (short) 1),
                    new NewTopic(DLT, 1, (short) 1))
            ).all().get(10, TimeUnit.SECONDS);
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name",
                POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add("asterion.order.payment-outcome.kafka.topic", () -> TOPIC);
        registry.add("asterion.order.payment-outcome.kafka.consumer.group-id",
                () -> "order-payment-reliability-test");
        registry.add("asterion.order.payment-outcome.kafka.retry.max-retries", () -> "2");
        registry.add("asterion.order.payment-outcome.kafka.retry.backoff-interval-ms",
                () -> "100");
    }

    @Test
    void shouldDeadLetterMalformedPayloadWithoutCallingUseCase() throws Exception {
        String key = UUID.randomUUID().toString();
        send(key, "payment.succeeded.v1", "{invalid-json");

        ConsumerRecord<String, String> deadLetter = awaitDeadLetter(key);

        assertThat(deadLetter.topic()).isEqualTo(DLT);
        assertThat(deadLetter.key()).isEqualTo(key);
        assertThat(deadLetter.value()).isEqualTo("{invalid-json");

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldRetryTransientFailureAndSucceedOnThirdAttempt() throws Exception {
        String key = UUID.randomUUID().toString();
        String payload = objectMapper.writeValueAsString(succeededEvent());

        RuntimeException temporaryFailure =
                new RuntimeException("Temporary database failure");

        doThrow(temporaryFailure)
                .doThrow(temporaryFailure)
                .doNothing()
                .when(useCase)
                .handle(any(PaymentSucceededEvent.class));

        send(key, "payment.succeeded.v1", payload);

        verify(useCase, timeout(15_000).times(3))
                .handle(any(PaymentSucceededEvent.class));
    }

    @Test
    void shouldDeadLetterRecordAfterRetryBudgetIsExhausted() throws Exception {
        String key = UUID.randomUUID().toString();
        String payload = objectMapper.writeValueAsString(succeededEvent());

        doThrow(new RuntimeException("Persistent temporary failure"))
                .when(useCase)
                .handle(any(PaymentSucceededEvent.class));

        send(key, "payment.succeeded.v1", payload);

        ConsumerRecord<String, String> deadLetter = awaitDeadLetter(key);

        assertThat(deadLetter.topic()).isEqualTo(DLT);
        assertThat(deadLetter.key()).isEqualTo(key);
        assertThat(deadLetter.value()).isEqualTo(payload);

        verify(useCase, timeout(5_000).times(3))
                .handle(any(PaymentSucceededEvent.class));
    }

    private void send(String key, String eventType, String payload) throws Exception {
        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, key, payload);

        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
    }

    private ConsumerRecord<String, String> awaitDeadLetter(String key) {
        Properties properties = new Properties();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG,
                "order-payment-dlt-test-" + UUID.randomUUID());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(Collections.singletonList(DLT));
            long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();

            while (System.nanoTime() < deadline) {
                var records = consumer.poll(Duration.ofMillis(250));
                for (ConsumerRecord<String, String> record : records) {
                    if (key.equals(record.key())) {
                        return record;
                    }
                }
            }
        }

        throw new AssertionError("No dead-letter record received for key "
                + key + " within 20 seconds");
    }

    private PaymentSucceededEvent succeededEvent() {
        return new PaymentSucceededEvent(
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.0000"));
    }
}
