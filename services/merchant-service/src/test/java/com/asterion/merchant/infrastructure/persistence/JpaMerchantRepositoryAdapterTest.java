package com.asterion.merchant.infrastructure.persistence;

import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import com.asterion.merchant.domain.model.MerchantStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class JpaMerchantRepositoryAdapterTest {

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
    private MerchantRepository merchantRepository;

    @Test
    void shouldPersistAndLoadMerchant() {
        UUID ownerUserId = UUID.randomUUID();
        Merchant merchant = Merchant.create(
                ownerUserId,
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );
        Merchant saved = merchantRepository.save(merchant);

        Optional<Merchant> loaded = merchantRepository.findById(saved.id());
        assertThat(loaded).isPresent();
        Merchant actual = loaded.orElseThrow();

        assertThat(actual.id()).isEqualTo(merchant.id());
        assertThat(actual.ownerUserId()).isEqualTo(ownerUserId);
        assertThat(actual.businessName())
                .isEqualTo("Asterion Technologies");
        assertThat(actual.legalName())
                .isEqualTo("Asterion Technologies Private Limited");
        assertThat(actual.contactEmail())
                .isEqualTo("merchant@example.com");
        assertThat(actual.status())
                .isEqualTo(MerchantStatus.PENDING);
        assertThat(actual.createdAt())
                .isEqualTo(merchant.createdAt());
    }

    @Test
    void shouldReturnEmptyWhenMerchantDoesNotExist() {
        Optional<Merchant> result = merchantRepository.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }
}
