package com.asterion.merchant.infrastructure.persistence;

import com.asterion.merchant.domain.model.MerchantStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchant")
public class MerchantJpaEntity {

    @Id
    private UUID merchantId;

    @Column(nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String legalName;

    @Column(nullable = false)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MerchantStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected MerchantJpaEntity() {
    }

    public MerchantJpaEntity(UUID merchantId, UUID ownerUserId,
                             String businessName, String legalName,
                             String contactEmail, MerchantStatus status,
                             Instant createdAt) {
        this.merchantId = merchantId;
        this.ownerUserId = ownerUserId;
        this.businessName = businessName;
        this.legalName = legalName;
        this.contactEmail = contactEmail;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public MerchantStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}