package com.logspectra.exception;

import com.logspectra.config.MdcKeys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Starter-provided global exception handler.
 *
 * <p><strong>Design intent:</strong> This advice exists purely to guarantee
 * that every unhandled exception is <em>logged</em> with full MDC context
 * before a response is returned.  It does <strong>not</strong> attempt to own
 * the application's error-response format.
 *
 * <p>If the consuming application defines its own {@code @RestControllerAdvice}
 * that is {@link Order ordered higher} (lower ordinal), that advice takes
 * precedence and this one will never be invoked for exceptions it handles.
 * This starter's advice is ordered at {@code Integer.MAX_VALUE - 1} to be
 * the last resort.
 *
 * <p>A minimal RFC-7807-style body is returned so that the caller receives
 * a machine-readable error even when no application-level handler exists.
 *
 * <p>Only active when running inside a Servlet web application
 * ({@code @ConditionalOnWebApplication}).
 */
@RestControllerAdvice
@Order(Integer.MAX_VALUE - 1)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionLoggingHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionLoggingHandler.class);

    /**
     * Catch-all handler.  Logs the exception with MDC context that was
     * populated by {@link com.logspectra.filter.LoggingFilter} and returns
     * a generic 500 error body.
     *
     * @param ex      the unhandled exception
     * @param request the current HTTP request (for additional context)
     * @return a {@code 500 Internal Server Error} response with a structured body
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(
            Exception ex,
            HttpServletRequest request) {

        String traceId   = MDC.get(MdcKeys.TRACE_ID);
        String service   = MDC.get(MdcKeys.SERVICE);
        String endpoint  = MDC.get(MdcKeys.ENDPOINT);
        String method    = MDC.get(MdcKeys.METHOD);

        // Structured error log — all MDC fields are automatically included
        // in the JSON payload by logstash-logback-encoder.
        log.error(
            "Unhandled exception [traceId={}, service={}, {}: {}] — {}",
            traceId,
            service,
            method,
            endpoint,
            ex.getMessage(),
            ex
        );

        Map<String, Object> body = buildErrorBody(ex, request, traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Builds a minimal RFC-7807-inspired error body.
     *
     * <p>Applications are free to override this handler and return their own
     * richer error format — this default exists only as a safety net.
     */
    private Map<String, Object> buildErrorBody(
            Exception ex,
            HttpServletRequest request,
            String traceId) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status",    HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error",     HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        // Avoid leaking internal exception messages in production by only
        // including the simple class name. Adjust to ex.getMessage() if
        // you prefer verbose error bodies in controlled environments.
        body.put("exception", ex.getClass().getSimpleName());
        body.put("path",      request.getRequestURI());

        if (traceId != null) {
            body.put("traceId", traceId);
        }

        return body;
    }
}
