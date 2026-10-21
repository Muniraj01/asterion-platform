package com.asterion.order.config;

import com.asterion.order.application.port.in.CreateOrderUseCase;
import com.asterion.order.application.port.in.GetOrderUseCase;
import com.asterion.order.application.port.in.HandlePaymentOutcomeUseCase;
import com.asterion.order.application.port.in.ListOrdersUseCase;
import com.asterion.order.application.port.in.TransitionOrderUseCase;
import com.asterion.order.application.port.out.EventPublisher;
import com.asterion.order.application.port.out.MerchantValidationPort;
import com.asterion.order.application.port.out.OrderOutboxRepository;
import com.asterion.order.application.port.out.OrderRepository;
import com.asterion.order.application.port.out.PaymentInitiationPort;
import com.asterion.order.application.service.CreateOrderService;
import com.asterion.order.application.service.GetOrderService;
import com.asterion.order.application.service.HandlePaymentOutcomeService;
import com.asterion.order.application.service.ListOrdersService;
import com.asterion.order.application.service.OrderOutboxPublisher;
import com.asterion.order.application.service.TransitionOrderService;
import com.asterion.order.infrastructure.messaging.PermanentPaymentOutcomeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;

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
    TransitionOrderUseCase transitionOrderUseCase(
            OrderRepository orderRepository) {
        return new TransitionOrderService(orderRepository);
    }

    @Bean
    HandlePaymentOutcomeUseCase handlePaymentOutcomeUseCase(
            OrderRepository orderRepository) {
        return new HandlePaymentOutcomeService(orderRepository);
    }

    @Bean
    DefaultErrorHandler orderPaymentOutcomeErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${asterion.order.payment-outcome.kafka.retry.backoff-interval-ms:1000}")
            long backoffIntervalMs,
            @Value("${asterion.order.payment-outcome.kafka.retry.max-retries:3}")
            long maxRetries) {

        if (backoffIntervalMs < 1) {
            throw new IllegalArgumentException(
                    "Payment outcome retry backoff must be at least 1 ms");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException(
                    "Payment outcome max retries must not be negative");
        }

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate, (record, exception) ->
                new TopicPartition(record.topic() + ".DLT", record.partition())
        );

        recoverer.setFailIfSendResultIsError(true);
        recoverer.setWaitForSendResultTimeout(Duration.ofSeconds(10));

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer, new FixedBackOff(backoffIntervalMs, maxRetries));

        errorHandler.addNotRetryableExceptions(
                PermanentPaymentOutcomeException.class,
                IllegalArgumentException.class,
                IllegalStateException.class
        );

        return errorHandler;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String>
    orderPaymentOutcomeKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler orderPaymentOutcomeErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(orderPaymentOutcomeErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
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
