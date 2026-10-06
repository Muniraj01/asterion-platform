package com.asterion.order.infrastructure.persistence;

import com.asterion.order.domain.model.OrderStatus;
import com.asterion.order.infrastructure.persistence.jpa.OrderJpaEntity;
import com.asterion.order.infrastructure.persistence.jpa.SpringDataOrderJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
class OrderPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("asterion_order")
                    .withUsername("asterion")
                    .withPassword("asterion");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private SpringDataOrderJpaRepository repository;

    @Test
    void shouldPersistOrderSchema() {
        UUID orderId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        var entity = new OrderJpaEntity(
                orderId,
                merchantId,
                customerId,
                new BigDecimal("125.50"),
                OrderStatus.CREATED,
                Instant.now()
        );

        repository.saveAndFlush(entity);
        var loaded = repository.findById(orderId);

        assertTrue(loaded.isPresent());
        assertEquals(orderId, loaded.get().getOrderId());
        assertEquals(merchantId, loaded.get().getMerchantId());
        assertEquals(customerId, loaded.get().getCustomerId());
        assertEquals(new BigDecimal("125.50"), loaded.get().getTotalAmount());
    }

    @Test
    void shouldTransitionStatusOnlyWhenExpectedStatusMatches() {
        UUID orderId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        OrderJpaEntity entity = new OrderJpaEntity(
                orderId,
                merchantId,
                customerId,
                new BigDecimal("100.00"),
                OrderStatus.CREATED,
                Instant.now());
        repository.saveAndFlush(entity);

        int transitioned = repository
                .transitionStatus(orderId, OrderStatus.CREATED, OrderStatus.CANCELLED);
        assertThat(transitioned).isEqualTo(1);

        OrderJpaEntity updated = repository.findById(orderId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        int secondTransition = repository
                .transitionStatus(orderId, OrderStatus.CREATED, OrderStatus.COMPLETED);
        assertThat(secondTransition).isEqualTo(0);

        OrderJpaEntity finalEntity = repository.findById(orderId).orElseThrow();
        assertThat(finalEntity.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}