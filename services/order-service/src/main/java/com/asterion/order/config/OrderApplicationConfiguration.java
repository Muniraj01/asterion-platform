package com.asterion.order.config;

import com.asterion.order.application.port.in.CreateOrderUseCase;
import com.asterion.order.application.port.out.EventPublisher;
import com.asterion.order.application.port.out.OrderOutboxRepository;
import com.asterion.order.application.port.out.OrderRepository;
import com.asterion.order.application.service.CreateOrderService;
import com.asterion.order.application.service.OrderOutboxPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableScheduling
public class OrderApplicationConfiguration {

    @Bean
    public CreateOrderUseCase createOrderUseCase(
            OrderRepository orderRepository,
            OrderOutboxRepository orderOutboxRepository,
            ObjectMapper objectMapper) {

        return new CreateOrderService(
                orderRepository,
                orderOutboxRepository,
                objectMapper
        );
    }

    @Bean
    public OrderOutboxPublisher orderOutboxPublisher(
            OrderOutboxRepository orderOutboxRepository,
            EventPublisher eventPublisher,
            @Value("${asterion.order.outbox.claim-lease-seconds:60}")
            long claimLeaseSeconds) {

        return new OrderOutboxPublisher(
                orderOutboxRepository,
                eventPublisher,
                Clock.systemUTC(),
                Duration.ofSeconds(claimLeaseSeconds)
        );
    }
}