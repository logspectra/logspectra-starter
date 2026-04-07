package com.logspectra.autoconfigure;

import com.logspectra.config.KafkaAppenderConfig;
import com.logspectra.exception.GlobalExceptionLoggingHandler;
import com.logspectra.filter.LoggingFilter;
import com.logspectra.properties.LogSpectraProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/**
 * Spring Boot auto-configuration entry point for the LogSpectra starter.
 *
 * <p>This class wires together all LogSpectra beans when the following
 * conditions are met:
 * <ol>
 *   <li>{@code logspectra.enabled} is {@code true} (default)</li>
 *   <li>The application is a Servlet-based web application</li>
 * </ol>
 *
 * <p>All beans are annotated with {@link ConditionalOnMissingBean} so that
 * application-level overrides are always respected — the starter never
 * forces its defaults onto consumers that have their own implementations.
 *
 * <p>Discovered automatically via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix  = "logspectra",
        name    = "enabled",
        havingValue = "true",
        matchIfMissing = true   // enabled by default — opt-out, not opt-in
)
@EnableConfigurationProperties(LogSpectraProperties.class)
public class LogSpectraAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LogSpectraAutoConfiguration.class);

    // ------------------------------------------------------------------ //
    //  Filter                                                             //
    // ------------------------------------------------------------------ //

    /**
     * Registers the {@link LoggingFilter} as a Servlet filter.
     *
     * <p>The registration bean is used instead of {@code @Component} so that
     * the filter order and URL patterns are under the starter's control,
     * not Spring's default discovery order.
     *
     * @param properties the bound LogSpectra configuration
     * @return a {@link FilterRegistrationBean} wrapping {@link LoggingFilter}
     */
    @Bean
    @ConditionalOnMissingBean(LoggingFilter.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<LoggingFilter> loggingFilterRegistration(
            LogSpectraProperties properties) {

        LoggingFilter filter = new LoggingFilter(properties);

        FilterRegistrationBean<LoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");          // intercept every request
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("logSpectraLoggingFilter");

        log.debug("[LogSpectra] LoggingFilter registered for service '{}'",
                properties.getServiceName());

        return registration;
    }

    // ------------------------------------------------------------------ //
    //  Kafka Appender                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Programmatically attaches a Logback Kafka appender backed by the
     * Logstash JSON encoder to the root logger.
     *
     * <p>This guarantees Kafka JSON logging even if the consumer has their
     * own {@code logback-spring.xml}. The programmatic setup is <em>additive</em>:
     * it never removes or overrides consumer-defined appenders.
     *
     * <p>Respects {@code logspectra.kafka.enabled} property to allow consumers
     * to disable Kafka shipping if needed.
     *
     * @param properties the bound LogSpectra configuration
     * @return a {@link KafkaAppenderConfig} that self-registers on {@code @PostConstruct}
     */
    @Bean
    @ConditionalOnMissingBean(KafkaAppenderConfig.class)
    @ConditionalOnProperty(prefix = "logspectra.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
    public KafkaAppenderConfig kafkaAppenderConfig(LogSpectraProperties properties, Environment environment) {
        return new KafkaAppenderConfig(properties, environment);
    }

    // ------------------------------------------------------------------ //
    //  Global Exception Handler                                           //
    // ------------------------------------------------------------------ //

    /**
     * Registers the starter's catch-all exception logger.
     *
     * <p>Ordered at the lowest priority so that application-defined
     * {@code @RestControllerAdvice} beans always win.
     *
     * @return a {@link GlobalExceptionLoggingHandler} bean
     */
    @Bean
    @ConditionalOnMissingBean(GlobalExceptionLoggingHandler.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public GlobalExceptionLoggingHandler globalExceptionLoggingHandler() {
        return new GlobalExceptionLoggingHandler();
    }
}
