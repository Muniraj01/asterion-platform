package com.asterion.payment.config;

import com.asterion.payment.application.port.in.InitiatePaymentUseCase;
import com.asterion.payment.application.port.out.PaymentRepository;
import com.asterion.payment.application.service.InitiatePaymentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
public class PaymentApplicationConfiguration {

    @Bean
    InitiatePaymentUseCase initiatePaymentUseCase(PaymentRepository paymentRepository) {
        return new InitiatePaymentService(paymentRepository);
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