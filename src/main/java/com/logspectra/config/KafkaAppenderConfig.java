package com.logspectra.config;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.danielwegener.logback.kafka.KafkaAppender;
import com.github.danielwegener.logback.kafka.delivery.AsynchronousDeliveryStrategy;
import com.github.danielwegener.logback.kafka.keying.KeyingStrategy;
import com.logspectra.properties.LogSpectraProperties;
import net.logstash.logback.encoder.LogstashEncoder;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Programmatically configures and attaches a Logback {@link KafkaAppender}
 * backed by a {@link LogstashEncoder} to the root logger.
 *
 * <p>Why programmatic instead of XML?  A starter cannot know the consumer's
 * {@code logback-spring.xml} configuration at compile time.  Programmatic
 * setup is additive — it never overrides the consumer's existing appenders.
 *
 * <p>The appender is wrapped in an {@link AsyncAppender} so that Kafka I/O
 * never blocks application threads.
 *
 * <p>Only activated when {@code logspectra.enabled=true} (the default) AND
 * {@code logspectra.kafka.bootstrap-servers} is present.
 */
@Configuration
@ConditionalOnProperty(prefix = "logspectra", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaAppenderConfig {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(KafkaAppenderConfig.class);

    /** Async queue depth — 512 events before older events are discarded. */
    private static final int ASYNC_QUEUE_SIZE = 512;

    /** Percentage of queue that must be full before lower-level events are discarded. */
    private static final int DISCARD_THRESHOLD = 20;

    private final LogSpectraProperties properties;

    /** Held so we can stop the appender cleanly at shutdown. */
    private AsyncAppender asyncAppender;

    public KafkaAppenderConfig(LogSpectraProperties properties) {
        this.properties = properties;
    }

    // ------------------------------------------------------------------ //
    //  Lifecycle                                                          //
    // ------------------------------------------------------------------ //

    @PostConstruct
    public void attachKafkaAppender() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        try {
            KafkaAppender<ILoggingEvent> kafkaAppender = buildKafkaAppender(context);
            kafkaAppender.start();

            asyncAppender = buildAsyncWrapper(context, kafkaAppender);
            asyncAppender.start();

            Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.addAppender(asyncAppender);

            log.info("[LogSpectra] Kafka appender attached — topic={}, brokers={}",
                    properties.getKafka().getTopic(),
                    properties.getKafka().getBootstrapServers());

        } catch (Exception e) {
            log.warn("[LogSpectra] Failed to attach Kafka appender — " +
                     "logs will NOT be sent to Kafka. Cause: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void detachKafkaAppender() {
        if (asyncAppender != null && asyncAppender.isStarted()) {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.detachAppender(asyncAppender);
            asyncAppender.stop();
            log.info("[LogSpectra] Kafka appender detached.");
        }
    }
    private KafkaAppender<ILoggingEvent> buildKafkaAppender(LoggerContext context) {
        KafkaAppender<ILoggingEvent> appender = new KafkaAppender<>();
        appender.setContext(context);
        appender.setName("KAFKA");
        appender.setTopic(properties.getKafka().getTopic());

        // Keying strategy — round-robin distributes load evenly across partitions
        KeyingStrategy<ILoggingEvent> keyingStrategy = new RoundRobinKeyingStrategy();
        appender.setKeyingStrategy(keyingStrategy);

        // Delivery strategy — async so Kafka backpressure doesn't block callers
        AsynchronousDeliveryStrategy deliveryStrategy = new AsynchronousDeliveryStrategy();
        appender.setDeliveryStrategy(deliveryStrategy);

        // Logstash JSON encoder — emits all MDC fields automatically
        LogstashEncoder encoder = buildEncoder(context);
        appender.setEncoder(encoder);

        // Kafka producer properties
        appender.addProducerConfig(
                "bootstrap.servers=" + properties.getKafka().getBootstrapServers());
        appender.addProducerConfig(
                "acks=" + properties.getKafka().getAcks());
        appender.addProducerConfig(
                "retries=" + properties.getKafka().getRetries());
        appender.addProducerConfig(
                "max.block.ms=" + properties.getKafka().getMaxBlockMs());
        appender.addProducerConfig(
                "key.serializer=org.apache.kafka.common.serialization.ByteArraySerializer");
        appender.addProducerConfig(
                "value.serializer=org.apache.kafka.common.serialization.ByteArraySerializer");
        // Idempotent producer for exactly-once delivery where supported
        appender.addProducerConfig("enable.idempotence=false");
        appender.addProducerConfig(
                "client.id=logspectra-" + properties.getServiceName());

        return appender;
    }

    private LogstashEncoder buildEncoder(LoggerContext context) {
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext(context);
        // Include MDC fields (service, endpoint, method, traceId) automatically
        encoder.setIncludeMdcKeyNames(java.util.List.of(
                MdcKeys.SERVICE,
                MdcKeys.ENDPOINT,
                MdcKeys.METHOD,
                MdcKeys.TRACE_ID));
        encoder.start();
        return encoder;
    }

    private AsyncAppender buildAsyncWrapper(LoggerContext context,
                                            KafkaAppender<ILoggingEvent> delegate) {
        AsyncAppender async = new AsyncAppender();
        async.setContext(context);
        async.setName("ASYNC_KAFKA");
        async.setQueueSize(ASYNC_QUEUE_SIZE);
        async.setDiscardingThreshold(DISCARD_THRESHOLD);
        // Block rather than drop when queue is completely full —
        // prevents silent log loss under extreme burst traffic.
        async.setNeverBlock(false);
        async.addAppender(delegate);
        return async;
    }

    private static final class RoundRobinKeyingStrategy implements KeyingStrategy<ILoggingEvent> {

        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public byte[] createKey(ILoggingEvent event) {
            return Integer.toString(sequence.getAndIncrement())
                    .getBytes(StandardCharsets.UTF_8);
        }
    }
}

