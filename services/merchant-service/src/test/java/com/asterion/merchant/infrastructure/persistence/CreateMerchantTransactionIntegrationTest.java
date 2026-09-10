package com.asterion.merchant.infrastructure.persistence;

import com.asterion.merchant.application.command.CreateMerchantCommand;
import com.asterion.merchant.application.port.in.CreateMerchantUseCase;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import com.asterion.merchant.application.port.out.MerchantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Import(CreateMerchantTransactionIntegrationTest.FailingOutboxConfiguration.class)
class CreateMerchantTransactionIntegrationTest {

    private static final AtomicReference<UUID> attemptedMerchantId =
            new AtomicReference<>();

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
    private CreateMerchantUseCase createMerchantUseCase;

    @Autowired
    private MerchantRepository merchantRepository;

    @Test
    void shouldRollbackMerchantWhenOutboxWriteFails() {
        UUID ownerUserId = UUID.randomUUID();
        CreateMerchantCommand command = new CreateMerchantCommand(
                ownerUserId,
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );

        assertThatThrownBy(() -> createMerchantUseCase.create(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Simulated outbox failure");

        UUID merchantId = attemptedMerchantId.get();
        assertThat(merchantId).isNotNull();
        assertThat(merchantRepository.findById(merchantId)).isEmpty();
    }

    @TestConfiguration
    static class FailingOutboxConfiguration {

        @Bean
        @Primary
        MerchantOutboxRepository failingMerchantOutboxRepository() {
            return event -> {
                attemptedMerchantId.set(event.aggregateId());
                throw new IllegalStateException("Simulated outbox failure");
            };
        }
    }
}