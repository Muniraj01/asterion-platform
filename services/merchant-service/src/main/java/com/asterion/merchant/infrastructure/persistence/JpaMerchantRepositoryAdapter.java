package com.asterion.merchant.infrastructure.persistence;

import com.asterion.merchant.application.model.MerchantPage;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
        return toDomain(saved);
    }

    @Override
    public Optional<Merchant> findById(UUID merchantId) {
        return repository
                .findById(merchantId)
                .map(this::toDomain);
    }

    @Override
    public MerchantPage findByOwnerUserId(UUID ownerUserId, int page, int size) {
        if (ownerUserId == null)
            throw new IllegalArgumentException("ownerUserId must not be null");
        
        if (page < 0)
            throw new IllegalArgumentException("page must not be negative");

        if (size <= 0)
            throw new IllegalArgumentException("size must be greater than zero");


        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"),
                        Sort.Order.desc("merchantId"))
        );

        var result = repository.findByOwnerUserId(ownerUserId, pageable);

        return new MerchantPage(
                result.getContent()
                        .stream()
                        .map(this::toDomain)
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private Merchant toDomain(MerchantJpaEntity entity) {
        return Merchant.reconstitute(
                entity.getMerchantId(),
                entity.getOwnerUserId(),
                entity.getBusinessName(),
                entity.getLegalName(),
                entity.getContactEmail(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}