package com.logspectra.config;

import com.logspectra.properties.LogSpectraProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KafkaAppenderConfig")
class KafkaAppenderConfigTest {

    @Test
    @DisplayName("should use default environment when no Spring profile is active")
    void defaultsEnvironmentToDefault() {
        LogSpectraProperties props = new LogSpectraProperties();
        props.setProjectId("project-123");

        KafkaAppenderConfig config = new KafkaAppenderConfig(props, new MockEnvironment());

        assertThat(config.resolveEnvironment()).isEqualTo("default");
    }

    @Test
    @DisplayName("should use active Spring profiles as environment")
    void usesActiveProfilesAsEnvironment() {
        LogSpectraProperties props = new LogSpectraProperties();
        props.setProjectId("project-123");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev", "aws");

        KafkaAppenderConfig config = new KafkaAppenderConfig(props, environment);

        assertThat(config.resolveEnvironment()).isEqualTo("dev,aws");
    }

    @Test
    @DisplayName("should include projectId and environment in custom JSON fields")
    void buildsCustomFieldsJson() {
        LogSpectraProperties props = new LogSpectraProperties();
        props.setServiceName("orders-service");
        props.setProjectId("project-123");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        KafkaAppenderConfig config = new KafkaAppenderConfig(props, environment);

        assertThat(config.buildCustomFieldsJson())
                .contains("\"projectId\":\"project-123\"")
                .contains("\"environment\":\"prod\"")
                .contains("\"service\":\"orders-service\"")
                .contains("\"host\":\"");
    }
}

