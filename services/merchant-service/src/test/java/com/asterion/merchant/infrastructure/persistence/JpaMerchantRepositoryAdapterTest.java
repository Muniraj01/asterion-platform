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

import java.time.Instant;
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

    @Test
    void shouldFindOnlyOwnerMerchantsInCreatedAtDescendingOrderWithPagination() {
        UUID ownerUserId = UUID.randomUUID();
        UUID otherOwnerUserId = UUID.randomUUID();

        Merchant oldest = Merchant.reconstitute(
                UUID.randomUUID(),
                ownerUserId,
                "Oldest",
                "Oldest Legal",
                "oldest@example.com",
                MerchantStatus.PENDING,
                Instant.parse("2026-01-01T10:00:00Z")
        );

        Merchant newest = Merchant.reconstitute(
                UUID.randomUUID(),
                ownerUserId,
                "Newest",
                "Newest Legal",
                "newest@example.com",
                MerchantStatus.PENDING,
                Instant.parse("2026-01-01T10:02:00Z")
        );

        Merchant middle = Merchant.reconstitute(
                UUID.randomUUID(),
                ownerUserId,
                "Middle",
                "Middle Legal",
                "middle@example.com",
                MerchantStatus.PENDING,
                Instant.parse("2026-01-01T10:01:00Z")
        );

        Merchant otherOwner = Merchant.reconstitute(
                UUID.randomUUID(),
                otherOwnerUserId,
                "Other",
                "Other Legal",
                "other@example.com",
                MerchantStatus.PENDING,
                Instant.parse("2026-01-01T10:03:00Z")
        );

        merchantRepository.save(oldest);
        merchantRepository.save(newest);
        merchantRepository.save(middle);
        merchantRepository.save(otherOwner);

        var firstPage = merchantRepository.findByOwnerUserId(ownerUserId, 0, 2);

        assertThat(firstPage.content())
                .extracting(Merchant::id)
                .containsExactly(newest.id(), middle.id());

        assertThat(firstPage.page()).isEqualTo(0);
        assertThat(firstPage.size()).isEqualTo(2);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);

        var secondPage = merchantRepository.findByOwnerUserId(ownerUserId, 1, 2);

        assertThat(secondPage.content())
                .extracting(Merchant::id)
                .containsExactly(oldest.id());

        assertThat(secondPage.page()).isEqualTo(1);
        assertThat(secondPage.size()).isEqualTo(2);
        assertThat(secondPage.totalElements()).isEqualTo(3);
        assertThat(secondPage.totalPages()).isEqualTo(2);
    }
}
