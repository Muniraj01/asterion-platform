package com.asterion.order.infrastructure.persistence;

import com.asterion.order.application.port.out.OrderRepository;
import com.asterion.order.domain.model.Order;
import com.asterion.order.infrastructure.persistence.jpa.OrderJpaEntity;
import com.asterion.order.infrastructure.persistence.jpa.SpringDataOrderJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class OrderRepositoryAdapter implements OrderRepository {

    private final SpringDataOrderJpaRepository repository;

    public OrderRepositoryAdapter(SpringDataOrderJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity(
                order.id(),
                order.merchantId(),
                order.customerId(),
                order.totalAmount(),
                order.status(),
                order.createdAt()
        );

        OrderJpaEntity saved = repository.save(entity);

        return Order.reconstitute(
                saved.getOrderId(),
                saved.getMerchantId(),
                saved.getCustomerId(),
                saved.getTotalAmount(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }

    @Override
    public boolean existsById(UUID orderId) {
        return repository.existsById(orderId);
    }
}