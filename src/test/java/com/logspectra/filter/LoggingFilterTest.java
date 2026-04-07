package com.logspectra.filter;

import com.logspectra.config.MdcKeys;
import com.logspectra.properties.LogSpectraProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingFilter")
class LoggingFilterTest {

    @Mock
    private FilterChain filterChain;

    private LogSpectraProperties properties;
    private LoggingFilter         filter;

    @BeforeEach
    void setUp() {
        properties = new LogSpectraProperties();
        properties.setServiceName("test-service");
        filter = new LoggingFilter(properties);
        MDC.clear();
    }

    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("should populate MDC with service, endpoint, method and traceId")
    void shouldPopulateMdc() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Capture MDC state during filter execution
        doAnswer(invocation -> {
            assertThat(MDC.get(MdcKeys.SERVICE)).isEqualTo("test-service");
            assertThat(MDC.get(MdcKeys.ENDPOINT)).isEqualTo("/api/users");
            assertThat(MDC.get(MdcKeys.METHOD)).isEqualTo("GET");
            assertThat(MDC.get(MdcKeys.TRACE_ID)).isNotBlank();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("should propagate traceId from X-Trace-Id header")
    void shouldPropagateTraceIdFromHeader() throws Exception {
        String expectedTraceId = "abc-123-fixed-trace";

        MockHttpServletRequest  request  = new MockHttpServletRequest("POST", "/api/orders");
        request.addHeader(MdcKeys.TRACE_ID_HEADER, expectedTraceId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(MDC.get(MdcKeys.TRACE_ID)).isEqualTo(expectedTraceId);
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);
    }

    @Test
    @DisplayName("should generate a new traceId when header is absent")
    void shouldGenerateTraceIdWhenHeaderAbsent() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(MDC.get(MdcKeys.TRACE_ID))
                    .isNotBlank()
                    .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);
    }

    @Test
    @DisplayName("should write traceId to X-Trace-Id response header")
    void shouldWriteTraceIdToResponseHeader() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest("GET", "/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(MdcKeys.TRACE_ID_HEADER)).isNotBlank();
    }

    @Test
    @DisplayName("should clear all MDC fields after request completes")
    void shouldClearMdcAfterRequest() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest("DELETE", "/api/items/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(MDC.get(MdcKeys.SERVICE)).isNull();
        assertThat(MDC.get(MdcKeys.ENDPOINT)).isNull();
        assertThat(MDC.get(MdcKeys.METHOD)).isNull();
        assertThat(MDC.get(MdcKeys.TRACE_ID)).isNull();
        assertThat(MDC.get(MdcKeys.SPAN_ID)).isNull();
    }

    @Test
    @DisplayName("should propagate spanId from X-B3-SpanId header")
    void shouldPropagateSpanIdFromHeader() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("X-B3-SpanId", "span-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(MDC.get(MdcKeys.SPAN_ID)).isEqualTo("span-123");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);
    }

    @Test
    @DisplayName("should clear MDC even when filterChain throws an exception")
    void shouldClearMdcOnException() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest("GET", "/boom");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doThrow(new RuntimeException("Unexpected!"))
                .when(filterChain).doFilter(request, response);

        assertThatNoException().isThrownBy(() -> {
            try {
                filter.doFilter(request, response, filterChain);
            } catch (RuntimeException ignored) { /* expected */ }
        });

        assertThat(MDC.get(MdcKeys.SERVICE)).isNull();
        assertThat(MDC.get(MdcKeys.TRACE_ID)).isNull();
    }
}
