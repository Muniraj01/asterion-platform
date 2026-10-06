package com.asterion.order.infrastructure.persistence;

import com.asterion.order.application.model.OrderPage;
import com.asterion.order.application.port.out.OrderRepository;
import com.asterion.order.domain.model.Order;
import com.asterion.order.domain.model.OrderStatus;
import com.asterion.order.infrastructure.persistence.jpa.OrderJpaEntity;
import com.asterion.order.infrastructure.persistence.jpa.SpringDataOrderJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
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
        return toDomain(saved);
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return repository.findById(orderId).map(this::toDomain);
    }

    @Override
    public OrderPage findByCustomerId(UUID customerId, int page, int size) {
        if (customerId == null)
            throw new NullPointerException("customerId must not be null");

        if (page < 0)
            throw new IllegalArgumentException("page must not be negative");

        if (size <= 0)
            throw new IllegalArgumentException("size must be greater than zero");

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("orderId")
                )
        );

        Page<OrderJpaEntity> result = repository.findByCustomerId(customerId, pageRequest);

        List<Order> orders = result
                .getContent()
                .stream()
                .map(this::toDomain)
                .toList();

        return new OrderPage(
                orders,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public boolean existsById(UUID orderId) {
        return repository.existsById(orderId);
    }

    private Order toDomain(OrderJpaEntity entity) {
        return Order.reconstitute(
                entity.getOrderId(),
                entity.getMerchantId(),
                entity.getCustomerId(),
                entity.getTotalAmount(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    @Override
    public boolean transitionStatus(UUID orderId,
                                    OrderStatus expectedStatus,
                                    OrderStatus targetStatus) {
        if (orderId == null)
            throw new NullPointerException("orderId must not be null");

        if (expectedStatus == null)
            throw new NullPointerException("expectedStatus must not be null");

        if (targetStatus == null)
            throw new NullPointerException("targetStatus must not be null");

        return repository.transitionStatus(orderId, expectedStatus, targetStatus) == 1;
    }
}