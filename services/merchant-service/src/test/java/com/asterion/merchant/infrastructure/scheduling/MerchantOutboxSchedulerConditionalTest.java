package com.asterion.merchant.infrastructure.scheduling;

import com.asterion.merchant.application.service.MerchantOutboxPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MerchantOutboxSchedulerConditionalTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldCreateSchedulerWhenPropertyIsTrue() {
        contextRunner
                .withPropertyValues("asterion.merchant.outbox.scheduler-enabled=true")
                .run(context ->
                    assertThat(context).hasSingleBean(MerchantOutboxScheduler.class));
    }

    @Test
    void shouldNotCreateSchedulerWhenPropertyIsFalse() {
        contextRunner
                .withPropertyValues("asterion.merchant.outbox.scheduler-enabled=false")
                .run(context ->
                    assertThat(context).doesNotHaveBean(MerchantOutboxScheduler.class));
    }

    @Test
    void shouldCreateSchedulerWhenPropertyIsMissing() {
        contextRunner
                .run(context ->
                    assertThat(context).hasSingleBean(MerchantOutboxScheduler.class));
    }

    @Configuration
    @Import(MerchantOutboxScheduler.class)
    static class TestConfiguration {

        @Bean
        MerchantOutboxPublisher merchantOutboxPublisher() {
            return mock(MerchantOutboxPublisher.class);
        }
    }
}