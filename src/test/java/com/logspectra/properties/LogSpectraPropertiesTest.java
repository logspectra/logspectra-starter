package com.logspectra.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogSpectraProperties")
class LogSpectraPropertiesTest {

    @Test
    @DisplayName("should have sensible defaults out-of-the-box")
    void defaultValues() {
        LogSpectraProperties props = new LogSpectraProperties();

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getServiceName()).isEqualTo("unknown-service");
        assertThat(props.getKafka()).isNotNull();
        assertThat(props.getKafka().getBootstrapServers()).isEqualTo("localhost:9092");
        assertThat(props.getKafka().getTopic()).isEqualTo("application-logs");
        assertThat(props.getKafka().getAcks()).isEqualTo("1");
        assertThat(props.getKafka().getRetries()).isEqualTo(3);
        assertThat(props.getKafka().getMaxBlockMs()).isEqualTo(2000);
    }

    @Test
    @DisplayName("should fall back to defaults when blank strings are set")
    void nullSafeSetters() {
        LogSpectraProperties props = new LogSpectraProperties();
        props.setServiceName("  ");
        props.getKafka().setBootstrapServers("");
        props.getKafka().setTopic(null);

        assertThat(props.getServiceName()).isEqualTo("unknown-service");
        assertThat(props.getKafka().getBootstrapServers()).isEqualTo("localhost:9092");
        assertThat(props.getKafka().getTopic()).isEqualTo("application-logs");
    }

    @Test
    @DisplayName("should accept and trim valid service name")
    void trimsServiceName() {
        LogSpectraProperties props = new LogSpectraProperties();
        props.setServiceName("  order-service  ");

        assertThat(props.getServiceName()).isEqualTo("order-service");
    }

    @Test
    @DisplayName("setKafka(null) should fall back to a default Kafka config")
    void nullKafkaFallback() {
        LogSpectraProperties props = new LogSpectraProperties();
        props.setKafka(null);

        assertThat(props.getKafka()).isNotNull();
        assertThat(props.getKafka().getTopic()).isEqualTo("application-logs");
    }
}
