package com.asterion.order.config;

import com.asterion.order.application.port.in.CreateOrderUseCase;
import com.asterion.order.application.port.in.GetOrderUseCase;
import com.asterion.order.application.port.in.ListOrdersUseCase;
import com.asterion.order.application.port.in.TransitionOrderUseCase;
import com.asterion.order.application.port.out.EventPublisher;
import com.asterion.order.application.port.out.MerchantValidationPort;
import com.asterion.order.application.port.out.OrderOutboxRepository;
import com.asterion.order.application.port.out.OrderRepository;
import com.asterion.order.application.port.out.PaymentInitiationPort;
import com.asterion.order.application.service.CreateOrderService;
import com.asterion.order.application.service.GetOrderService;
import com.asterion.order.application.service.ListOrdersService;
import com.asterion.order.application.service.OrderOutboxPublisher;
import com.asterion.order.application.service.TransitionOrderService;
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
    CreateOrderUseCase createOrderUseCase(
            OrderRepository orderRepository,
            MerchantValidationPort merchantValidationPort,
            OrderOutboxRepository orderOutboxRepository,
            PaymentInitiationPort paymentInitiationPort,
            ObjectMapper objectMapper) {

        return new CreateOrderService(
                merchantValidationPort,
                orderRepository,
                orderOutboxRepository,
                paymentInitiationPort,
                objectMapper
        );
    }

    @Bean
    GetOrderUseCase getOrderUseCase(OrderRepository orderRepository) {
        return new GetOrderService(orderRepository);
    }

    @Bean
    ListOrdersUseCase listOrdersUseCase(OrderRepository orderRepository) {
        return new ListOrdersService(orderRepository);
    }

    @Bean
    TransitionOrderUseCase transitionOrderUseCase(OrderRepository orderRepository) {
        return new TransitionOrderService(orderRepository);
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