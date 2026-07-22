package com.fiap.feedbacks.api.web.error;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * Complemento JAX-RS: propaga X-Trace-Id no MDC quando o request chega à camada REST.
 * O filtro Vert.x ({@link TraceIdFilterRegistrar}) cobre falhas de auth precoces.
 */
@Provider
@PreMatching
public class TraceIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String TRACE_ID_HEADER = TraceIdFilterRegistrar.TRACE_ID_HEADER;
    public static final String TRACE_ID_MDC_KEY = TraceIdFilterRegistrar.TRACE_ID_MDC_KEY;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        var traceId = requestContext.getHeaderString(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            Object existing = MDC.get(TRACE_ID_MDC_KEY);
            traceId = existing != null ? existing.toString() : UUID.randomUUID().toString();
        }
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        requestContext.setProperty(TRACE_ID_MDC_KEY, traceId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        Object traceId = requestContext.getProperty(TRACE_ID_MDC_KEY);
        if (traceId == null) {
            traceId = MDC.get(TRACE_ID_MDC_KEY);
        }
        if (traceId != null && !responseContext.getHeaders().containsKey(TRACE_ID_HEADER)) {
            responseContext.getHeaders().putSingle(TRACE_ID_HEADER, traceId.toString());
        }
    }
}
