package com.asterion.merchant.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Merchant {

    private final UUID merchantId;
    private final UUID ownerUserId;
    private final String businessName;
    private final String legalName;
    private final String contactEmail;
    private final Instant createdAt;
    private MerchantStatus status;

    private Merchant(UUID merchantId, UUID ownerUserId,
                     String businessName, String legalName,
                     String contactEmail, MerchantStatus status,
                     Instant createdAt) {
        this.merchantId = Objects.requireNonNull(merchantId, "merchantId must not be null");
        if (ownerUserId == null) {
            throw new IllegalArgumentException("ownerUserId must not be null");
        }
        this.ownerUserId = ownerUserId;

        this.businessName = requireText(businessName, "businessName");
        this.legalName = requireText(legalName, "legalName");
        this.contactEmail = requireText(contactEmail, "contactEmail");

        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Merchant create(UUID ownerUserId, String businessName,
                                  String legalName, String contactEmail) {
        return new Merchant(UUID.randomUUID(), ownerUserId,
                businessName, legalName,
                contactEmail, MerchantStatus.PENDING,
                Instant.now());
    }

    public void activate() {
        requireStatus(MerchantStatus.PENDING, "Only a pending merchant can be activated");
        this.status = MerchantStatus.ACTIVE;
    }

    public void suspend() {
        requireStatus(MerchantStatus.ACTIVE, "Only an active merchant can be suspended");
        this.status = MerchantStatus.SUSPENDED;
    }

    public void reactivate() {
        requireStatus(MerchantStatus.SUSPENDED, "Only a suspended merchant can be reactivated");
        this.status = MerchantStatus.ACTIVE;
    }

    public void terminate() {
        requireStatus(MerchantStatus.ACTIVE, "Only an active merchant can be terminated");
        this.status = MerchantStatus.TERMINATED;
    }

    public UUID id() {
        return merchantId;
    }

    public UUID ownerUserId() {
        return ownerUserId;
    }

    public String businessName() {
        return businessName;
    }

    public String legalName() {
        return legalName;
    }

    public String contactEmail() {
        return contactEmail;
    }

    public MerchantStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    private void requireStatus(MerchantStatus expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

}