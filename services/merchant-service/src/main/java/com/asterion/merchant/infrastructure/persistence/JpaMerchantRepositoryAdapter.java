package com.asterion.merchant.infrastructure.persistence;

import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaMerchantRepositoryAdapter implements MerchantRepository {

    private final JpaMerchantRepository repository;

    public JpaMerchantRepositoryAdapter(JpaMerchantRepository repository) {
        this.repository = repository;
    }

    @Override
    public Merchant save(Merchant merchant) {
        MerchantJpaEntity entity = new MerchantJpaEntity(
                merchant.id(),
                merchant.ownerUserId(),
                merchant.businessName(),
                merchant.legalName(),
                merchant.contactEmail(),
                merchant.status(),
                merchant.createdAt()
        );

        MerchantJpaEntity saved = repository.save(entity);

        return Merchant.reconstitute(
                saved.getMerchantId(),
                saved.getOwnerUserId(),
                saved.getBusinessName(),
                saved.getLegalName(),
                saved.getContactEmail(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }

    @Override
    public Optional<Merchant> findById(UUID merchantId) {
        return repository.findById(merchantId)
                .map(entity -> Merchant.reconstitute(
                        entity.getMerchantId(),
                        entity.getOwnerUserId(),
                        entity.getBusinessName(),
                        entity.getLegalName(),
                        entity.getContactEmail(),
                        entity.getStatus(),
                        entity.getCreatedAt()
                ));
    }

}