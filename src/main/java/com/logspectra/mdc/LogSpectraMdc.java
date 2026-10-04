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
        String previousTraceId = MDC.get(MdcKeys.TRACE_ID);
        String previousSpanId = MDC.get(MdcKeys.SPAN_ID);
        try {
            MDC.put(MdcKeys.TRACE_ID, UUID.randomUUID().toString());
            action.run();
        } finally {
            restore(MdcKeys.TRACE_ID, previousTraceId);
            restore(MdcKeys.SPAN_ID, previousSpanId);
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
