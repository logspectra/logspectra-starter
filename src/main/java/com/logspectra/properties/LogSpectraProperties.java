package com.logspectra.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the LogSpectra structured logging starter.
 *
 * <p>All properties are bound under the {@code logspectra} prefix.
 *
 * <p>Example {@code application.yml}:
 * <pre>{@code
 * logspectra:
 *   enabled: true
 *   service-name: user-service
 *   project-id: project-123
 *   kafka:
 *     bootstrap-servers: localhost:9092
 *     topic: logs-topic
 * }</pre>
 */
@ConfigurationProperties(prefix = "logspectra")
@Validated
public class LogSpectraProperties {

    /**
     * Master switch. Set to {@code false} to disable all LogSpectra behaviour.
     * Defaults to {@code true}.
     */
    private boolean enabled = true;

    /**
     * Logical name of this microservice. Injected into every log record as
     * the {@code service} field. Falls back to {@code "unknown-service"} when
     * not set so logs are never emitted without a service identifier.
     */
    private String serviceName = "unknown-service";

    /**
     * Project or tenant identifier. Injected into every log record as
     * the {@code projectId} field. Required.
     */
    @NotBlank
    private String projectId;

    /**
     * Kafka-specific configuration block.
     */
    @NestedConfigurationProperty
    private Kafka kafka = new Kafka();

    // ------------------------------------------------------------------ //
    //  Nested: Kafka                                                     //
    // ------------------------------------------------------------------ //

    public static class Kafka {

        /**
         * Kafka-only switch. Set to {@code false} to keep console JSON logging
         * while disabling Kafka log shipping.
         */
        private boolean enabled = true;

        /**
         * Comma-separated list of Kafka broker addresses.
         * Example: {@code localhost:9092,broker2:9092}
         */
        private String bootstrapServers = "localhost:9092";

        /**
         * Kafka topic to which structured log records are published.
         */
        private String topic = "application-logs";

        /**
         * Maximum time (ms) the producer will block waiting for buffer space.
         * Keeping this low prevents slow Kafka from stalling HTTP threads.
         */
        private int maxBlockMs = 2000;

        /**
         * Acks configuration for Kafka producer reliability.
         * {@code 0} = fire-and-forget (fastest, lowest durability)
         * {@code 1} = leader ack (balanced — default)
         * {@code all} = full ISR ack (slowest, highest durability)
         */
        private String acks = "1";

        /**
         * Number of times the producer will retry a failed send.
         */
        private int retries = 3;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBootstrapServers() {
            return bootstrapServers;
        }

        public void setBootstrapServers(String bootstrapServers) {
            this.bootstrapServers = (bootstrapServers != null && !bootstrapServers.isBlank())
                    ? bootstrapServers.trim()
                    : "localhost:9092";
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = (topic != null && !topic.isBlank())
                    ? topic.trim()
                    : "application-logs";
        }

        public int getMaxBlockMs() {
            return maxBlockMs;
        }

        public void setMaxBlockMs(int maxBlockMs) {
            this.maxBlockMs = maxBlockMs > 0 ? maxBlockMs : 2000;
        }

        public String getAcks() {
            return acks;
        }

        public void setAcks(String acks) {
            this.acks = acks;
        }

        public int getRetries() {
            return retries;
        }

        public void setRetries(int retries) {
            this.retries = retries >= 0 ? retries : 3;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = (serviceName != null && !serviceName.isBlank())
                ? serviceName.trim()
                : "unknown-service";
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = (projectId != null && !projectId.isBlank()) ? projectId.trim() : null;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka != null ? kafka : new Kafka();
    }
}
