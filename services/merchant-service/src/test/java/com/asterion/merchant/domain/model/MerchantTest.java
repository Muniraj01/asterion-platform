package com.asterion.merchant.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class MerchantTest {

    @Test
    void shouldCreateMerchantInPendingStatus() {
        UUID ownerUserId = UUID.randomUUID();
        Instant before = Instant.now();
        Merchant merchant = Merchant.create(
                ownerUserId,
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );

        Instant after = Instant.now();

        assertThat(merchant.id()).isNotNull();
        assertThat(merchant.ownerUserId()).isEqualTo(ownerUserId);
        assertThat(merchant.businessName()).isEqualTo("Asterion Technologies");
        assertThat(merchant.legalName())
                .isEqualTo("Asterion Technologies Private Limited");
        assertThat(merchant.contactEmail())
                .isEqualTo("merchant@example.com");
        assertThat(merchant.status())
                .isEqualTo(MerchantStatus.PENDING);
        assertThat(merchant.createdAt())
                .isBetween(before, after);
    }

    @Test
    void shouldRejectMissingOwnerUserId() {
        assertThatThrownBy(() -> Merchant.create(
                null,
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com")
        )
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectBlankBusinessName() {
        assertThatThrownBy(() -> Merchant.create(
                UUID.randomUUID(),
                "",
                "Asterion Technologies Private Limited",
                "merchant@example.com")
        )
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectBlankLegalName() {
        assertThatThrownBy(() -> Merchant.create(
                UUID.randomUUID(),
                "Asterion Technologies",
                "",
                "merchant@example.com")
        )
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectBlankContactEmail() {
        assertThatThrownBy(() -> Merchant.create(
                UUID.randomUUID(),
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "")
        )
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldActivatePendingMerchant() {
        Merchant merchant = createMerchant();
        merchant.activate();
        assertThat(merchant.status()).isEqualTo(MerchantStatus.ACTIVE);
    }

    @Test
    void shouldSuspendActiveMerchant() {
        Merchant merchant = createMerchant();
        merchant.activate();
        merchant.suspend();
        assertThat(merchant.status()).isEqualTo(MerchantStatus.SUSPENDED);
    }

    @Test
    void shouldReactivateSuspendedMerchant() {
        Merchant merchant = createMerchant();
        merchant.activate();
        merchant.suspend();
        merchant.reactivate();
        assertThat(merchant.status()).isEqualTo(MerchantStatus.ACTIVE);
    }

    @Test
    void shouldTerminateActiveMerchant() {
        Merchant merchant = createMerchant();
        merchant.activate();
        merchant.terminate();
        assertThat(merchant.status()).isEqualTo(MerchantStatus.TERMINATED);
    }

    @Test
    void shouldNotTerminatePendingMerchant() {
        Merchant merchant = createMerchant();
        assertThatThrownBy(merchant::terminate)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldNotActivateTerminatedMerchant() {
        Merchant merchant = createMerchant();
        merchant.activate();
        merchant.terminate();
        assertThatThrownBy(merchant::activate)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldNotSuspendPendingMerchant() {
        Merchant merchant = createMerchant();
        assertThatThrownBy(merchant::suspend)
                .isInstanceOf(IllegalStateException.class);
    }

    private Merchant createMerchant() {
        return Merchant.create(
                UUID.randomUUID(),
                "Asterion Technologies",
                "Asterion Technologies Private Limited",
                "merchant@example.com"
        );
    }
}