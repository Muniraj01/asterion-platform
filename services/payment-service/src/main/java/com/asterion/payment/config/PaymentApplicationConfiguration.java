package com.asterion.payment.config;

import com.asterion.payment.application.port.in.InitiatePaymentUseCase;
import com.asterion.payment.application.port.in.ProcessPaymentUseCase;
import com.asterion.payment.application.port.out.PaymentProcessorPort;
import com.asterion.payment.application.port.out.PaymentRepository;
import com.asterion.payment.application.service.InitiatePaymentService;
import com.asterion.payment.application.service.ProcessPaymentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.time.Clock;

@Configuration
public class PaymentApplicationConfiguration {

    @Bean
    Clock paymentClock() {
        return Clock.systemUTC();
    }

    @Bean
    ProcessPaymentUseCase processPaymentUseCase(
            PaymentRepository paymentRepository,
            PaymentProcessorPort paymentProcessorPort,
            Clock paymentClock) {

        return new ProcessPaymentService(
                paymentRepository,
                paymentProcessorPort,
                paymentClock
        );
    }

    @Bean
    InitiatePaymentUseCase initiatePaymentUseCase(
            PaymentRepository paymentRepository,
            ProcessPaymentUseCase processPaymentUseCase) {

        return new InitiatePaymentService(
                paymentRepository,
                processPaymentUseCase
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String>
    paymentKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }
}