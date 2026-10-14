package com.asterion.payment.config;

import com.asterion.payment.application.port.in.InitiatePaymentUseCase;
import com.asterion.payment.application.port.in.ProcessPaymentUseCase;
import com.asterion.payment.application.port.out.PaymentProcessorPort;
import com.asterion.payment.application.port.out.PaymentRepository;
import com.asterion.payment.application.service.InitiatePaymentService;
import com.asterion.payment.application.service.ProcessPaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableScheduling
public class PaymentApplicationConfiguration {

    @Bean
    Clock paymentClock() {
        return Clock.systemUTC();
    }

    @Bean
    ProcessPaymentUseCase processPaymentUseCase(
            PaymentRepository paymentRepository,
            PaymentProcessorPort paymentProcessorPort,
            Clock paymentClock,
            @Value("${asterion.payment.processing.max-attempts:5}")
            int maxAttempts,
            @Value("${asterion.payment.processing.base-retry-delay-ms:5000}")
            long baseRetryDelayMs,
            @Value("${asterion.payment.processing.max-retry-delay-ms:300000}")
            long maxRetryDelayMs,
            @Value("${asterion.payment.processing.lease-duration-ms:60000}")
            long leaseDurationMs) {

        return new ProcessPaymentService(
                paymentRepository,
                paymentProcessorPort,
                paymentClock,
                maxAttempts,
                Duration.ofMillis(baseRetryDelayMs),
                Duration.ofMillis(maxRetryDelayMs),
                Duration.ofMillis(leaseDurationMs)
        );
    }

    @Bean
    InitiatePaymentUseCase initiatePaymentUseCase(
            PaymentRepository paymentRepository,
            ProcessPaymentUseCase processPaymentUseCase) {

        return new InitiatePaymentService(paymentRepository, processPaymentUseCase);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String>
    paymentKafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }
}