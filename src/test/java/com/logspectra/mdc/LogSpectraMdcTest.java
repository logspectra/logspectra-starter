package com.logspectra.mdc;

import com.logspectra.config.MdcKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class LogSpectraMdcTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("runWithNewTrace sets traceId for the duration of the action")
    void setsTraceDuringAction() {
        assertThat(MDC.get(MdcKeys.TRACE_ID)).isNull();

        LogSpectraMdc.runWithNewTrace(() ->
            assertThat(MDC.get(MdcKeys.TRACE_ID)).isNotBlank()
        );

        assertThat(MDC.get(MdcKeys.TRACE_ID)).isNull();
    }

    @Test
    @DisplayName("runWithNewTrace restores a pre-existing traceId")
    void restoresPreviousTrace() {
        MDC.put(MdcKeys.TRACE_ID, "parent-trace");

        LogSpectraMdc.runWithNewTrace(() ->
            assertThat(MDC.get(MdcKeys.TRACE_ID)).isNotEqualTo("parent-trace")
        );

    @Test
    @DisplayName("runWithRequestContext sets endpoint and method for analytics")
    void setsEndpointAndMethod() {
        LogSpectraMdc.runWithRequestContext("/api/checkout", "GET", () -> {
            assertThat(MDC.get(MdcKeys.ENDPOINT)).isEqualTo("/api/checkout");
            assertThat(MDC.get(MdcKeys.METHOD)).isEqualTo("GET");
            assertThat(MDC.get(MdcKeys.TRACE_ID)).isNotBlank();
        });

        assertThat(MDC.get(MdcKeys.ENDPOINT)).isNull();
        assertThat(MDC.get(MdcKeys.METHOD)).isNull();
    }
}
