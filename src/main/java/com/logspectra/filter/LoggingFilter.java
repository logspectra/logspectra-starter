package com.logspectra.filter;

import com.logspectra.config.MdcKeys;
import com.logspectra.properties.LogSpectraProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that enriches the SLF4J MDC with per-request context fields
 * before the request is processed, and clears them reliably after — even when
 * exceptions are thrown.
 *
 * <p>Fields written to MDC:
 * <ul>
 *   <li>{@code service}  — logical service name from {@link LogSpectraProperties}</li>
 *   <li>{@code endpoint} — request URI (e.g. {@code /api/orders/1})</li>
 *   <li>{@code method}   — HTTP method (e.g. {@code GET})</li>
 *   <li>{@code traceId}  — propagated from {@code X-Trace-Id} header, or a new UUID</li>
 * </ul>
 *
 * <p>The filter is ordered at {@link Ordered#HIGHEST_PRECEDENCE} so that context
 * is available to all downstream filters, interceptors, and controllers.
 *
 * <p>The generated / propagated {@code traceId} is also written back into the
 * HTTP response as an {@code X-Trace-Id} header so that clients can correlate
 * requests end-to-end.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    private final LogSpectraProperties properties;

    public LoggingFilter(LogSpectraProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         filterChain)
            throws ServletException, IOException {

        String traceId = resolveTraceId(request);

        try {
            populateMdc(request, traceId);

            // Echo trace ID back so callers can correlate their own logs
            response.setHeader(MdcKeys.TRACE_ID_HEADER, traceId);

            if (log.isDebugEnabled()) {
                log.debug("Incoming request: {} {}", request.getMethod(), request.getRequestURI());
            }

            filterChain.doFilter(request, response);

        } finally {
            /*
             * ALWAYS clear MDC — if we don't, thread-pool reuse will leak
             * context from one request into an unrelated future request.
             */
            clearMdc();
        }
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Returns the trace ID from the incoming header, or mints a fresh UUID.
     */
    private String resolveTraceId(HttpServletRequest request) {
        String fromHeader = request.getHeader(MdcKeys.TRACE_ID_HEADER);
        if (fromHeader != null && !fromHeader.isBlank()) {
            return fromHeader.trim();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Writes all context fields into the MDC for this thread.
     */
    private void populateMdc(HttpServletRequest request, String traceId) {
        MDC.put(MdcKeys.SERVICE,   properties.getServiceName());
        MDC.put(MdcKeys.ENDPOINT,  request.getRequestURI());
        MDC.put(MdcKeys.METHOD,    request.getMethod());
        MDC.put(MdcKeys.TRACE_ID,  traceId);
    }

    /**
     * Removes all LogSpectra MDC keys.  Scoped removal (rather than
     * {@code MDC.clear()}) is intentional: it avoids clobbering any MDC
     * entries set by other libraries on the same thread.
     */
    private void clearMdc() {
        MDC.remove(MdcKeys.SERVICE);
        MDC.remove(MdcKeys.ENDPOINT);
        MDC.remove(MdcKeys.METHOD);
        MDC.remove(MdcKeys.TRACE_ID);
    }
}
