package com.logspectra.config;

/**
 * Centralised MDC (Mapped Diagnostic Context) field-name constants.
 *
 * <p>Using constants eliminates magic-string scatter and ensures that the
 * filter, the logback encoder configuration, and any user code that
 * accesses MDC all agree on the same key names.
 */
public final class MdcKeys {

    // Prevent instantiation
    private MdcKeys() {}

    /** Name of the microservice emitting the log record. */
    public static final String SERVICE   = "service";

    /** Incoming HTTP request URI (e.g. {@code /api/users/42}). */
    public static final String ENDPOINT  = "endpoint";

    /** HTTP method of the incoming request (e.g. {@code GET}, {@code POST}). */
    public static final String METHOD    = "method";

    /**
     * Distributed trace identifier.
     * Taken from the {@code X-Trace-Id} request header when present,
     * otherwise a new UUID is generated per request.
     */
    public static final String TRACE_ID  = "traceId";

    /** Distributed span identifier (from tracing system or incoming headers). */
    public static final String SPAN_ID = "spanId";

    /** Exception class captured at log time for structured error records. */
    public static final String EXCEPTION = "exception";

    /** HTTP header name used to propagate/receive the trace identifier. */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
}
