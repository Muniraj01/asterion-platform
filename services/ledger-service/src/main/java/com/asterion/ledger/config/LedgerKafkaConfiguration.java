
package com.asterion.ledger.config;

import com.asterion.ledger.infrastructure.messaging.PermanentLedgerEventException;
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
import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;

@Configuration
public class LedgerKafkaConfiguration {

    @Bean
    DefaultErrorHandler ledgerKafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${asterion.ledger.kafka.retry.backoff-interval-ms:1000}")
            long backoffIntervalMs,
            @Value("${asterion.ledger.kafka.retry.max-retries:3}")
            long maxRetries) {

        if (backoffIntervalMs < 1 || maxRetries < 0)
            throw new IllegalArgumentException("Ledger retry backoff must be" +
                    " positive and retries must be non-negative");

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate, (record, exception) ->
                new TopicPartition(record.topic() + ".DLT", record.partition())
        );

        recoverer.setFailIfSendResultIsError(true);
        recoverer.setWaitForSendResultTimeout(Duration.ofSeconds(10));

        DefaultErrorHandler handler = new DefaultErrorHandler(
                recoverer, new FixedBackOff(backoffIntervalMs, maxRetries));

        handler.addNotRetryableExceptions(
                PermanentLedgerEventException.class,
                IllegalArgumentException.class,
                IllegalStateException.class
        );

        return handler;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String>
    ledgerKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler ledgerKafkaErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(ledgerKafkaErrorHandler);
        factory.getContainerProperties().setAckMode(
                ContainerProperties.AckMode.RECORD);

        return factory;
    }
}
