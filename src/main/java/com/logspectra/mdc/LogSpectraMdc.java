package com.logspectra.mdc;

import com.logspectra.config.MdcKeys;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * Helpers for non-HTTP code paths (scheduled jobs, async tasks) so Kafka JSON
 * includes {@code traceId} the same way {@code LoggingFilter} does for requests.
 */
public final class LogSpectraMdc {

    private LogSpectraMdc() {}

    /**
     * Runs {@code action} with a fresh trace id in MDC (restores previous value after).
     */
    public static void runWithNewTrace(Runnable action) {
        runWithRequestContext(null, null, true, action);
    }

    /**
     * Runs {@code action} with {@code endpoint} (and optional HTTP method) in MDC so Kafka JSON
     * includes routing context for analytics (e.g. top affected endpoints).
     */
    public static void runWithRequestContext(String endpoint, String httpMethod, Runnable action) {
        runWithRequestContext(endpoint, httpMethod, false, action);
    }

    private static void runWithRequestContext(
            String endpoint,
            String httpMethod,
            boolean newTrace,
            Runnable action) {
        String previousTraceId = MDC.get(MdcKeys.TRACE_ID);
        String previousSpanId = MDC.get(MdcKeys.SPAN_ID);
        String previousEndpoint = MDC.get(MdcKeys.ENDPOINT);
        String previousMethod = MDC.get(MdcKeys.METHOD);
        try {
            if (newTrace || previousTraceId == null || previousTraceId.isBlank()) {
                MDC.put(MdcKeys.TRACE_ID, UUID.randomUUID().toString());
            }
            if (endpoint != null && !endpoint.isBlank()) {
                MDC.put(MdcKeys.ENDPOINT, endpoint.trim());
            }
            if (httpMethod != null && !httpMethod.isBlank()) {
                MDC.put(MdcKeys.METHOD, httpMethod.trim());
            }
            action.run();
        } finally {
            restore(MdcKeys.TRACE_ID, previousTraceId);
            restore(MdcKeys.SPAN_ID, previousSpanId);
            restore(MdcKeys.ENDPOINT, previousEndpoint);
            restore(MdcKeys.METHOD, previousMethod);
        }
    }

    private static void restore(String key, String previous) {
        if (previous != null && !previous.isBlank()) {
            MDC.put(key, previous);
        } else {
            MDC.remove(key);
        }
    }
}
