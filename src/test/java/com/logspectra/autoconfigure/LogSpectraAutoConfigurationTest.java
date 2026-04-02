package com.logspectra.autoconfigure;

import com.logspectra.config.KafkaAppenderConfig;
import com.logspectra.exception.GlobalExceptionLoggingHandler;
import com.logspectra.filter.LoggingFilter;
import com.logspectra.properties.LogSpectraProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link LogSpectraAutoConfiguration} registers (or skips) beans
 * correctly under various property conditions.
 */
@DisplayName("LogSpectraAutoConfiguration")
class LogSpectraAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LogSpectraAutoConfiguration.class));

    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("should register all beans when logspectra.enabled=true (default)")
    void registersBeansWhenEnabled() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(LogSpectraProperties.class);
            assertThat(ctx).hasSingleBean(KafkaAppenderConfig.class);
            assertThat(ctx).hasSingleBean(GlobalExceptionLoggingHandler.class);
        });
    }

    @Test
    @DisplayName("should NOT register any beans when logspectra.enabled=false")
    void doesNotRegisterBeansWhenDisabled() {
        contextRunner
                .withPropertyValues("logspectra.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(LoggingFilter.class);
                    assertThat(ctx).doesNotHaveBean(KafkaAppenderConfig.class);
                    assertThat(ctx).doesNotHaveBean(GlobalExceptionLoggingHandler.class);
                });
    }

    @Test
    @DisplayName("should bind service-name from properties")
    void bindsServiceName() {
        contextRunner
                .withPropertyValues("logspectra.service-name=inventory-service")
                .run(ctx -> {
                    LogSpectraProperties props = ctx.getBean(LogSpectraProperties.class);
                    assertThat(props.getServiceName()).isEqualTo("inventory-service");
                });
    }

    @Test
    @DisplayName("should bind Kafka topic from properties")
    void bindsKafkaTopic() {
        contextRunner
                .withPropertyValues("logspectra.kafka.topic=custom-logs-topic")
                .run(ctx -> {
                    LogSpectraProperties props = ctx.getBean(LogSpectraProperties.class);
                    assertThat(props.getKafka().getTopic()).isEqualTo("custom-logs-topic");
                });
    }

    @Test
    @DisplayName("should NOT replace user-defined LoggingFilter with starter bean")
    void respectsUserDefinedLoggingFilter() {
        contextRunner
                .withBean("customLoggingFilter", LoggingFilter.class,
                        () -> new LoggingFilter(new LogSpectraProperties()))
                .run(ctx -> assertThat(ctx).hasSingleBean(LoggingFilter.class));
    }
}
