package com.fiap.feedbacks.api.web.error;

import io.quarkus.vertx.http.runtime.filters.Filters;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.MDC;

import java.util.UUID;

/**
 * Registra filtro HTTP cedo (Vert.x) para X-Trace-Id / MDC / contexto Vert.x,
 * cobrindo 401/403 emitidos pelo Quarkus Security antes dos filters JAX-RS.
 */
@ApplicationScoped
public class TraceIdFilterRegistrar {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String TRACE_ID_CTX_KEY = "app.traceId";

    void register(@Observes Filters filters) {
        filters.register(this::handle, 100);
    }

    private void handle(RoutingContext rc) {
        String traceId = rc.request().getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        rc.put(TRACE_ID_MDC_KEY, traceId);
        if (Vertx.currentContext() != null) {
            Vertx.currentContext().putLocal(TRACE_ID_CTX_KEY, traceId);
        }
        rc.response().putHeader(TRACE_ID_HEADER, traceId);

        rc.addBodyEndHandler(v -> {
            MDC.remove(TRACE_ID_MDC_KEY);
            if (Vertx.currentContext() != null) {
                Vertx.currentContext().removeLocal(TRACE_ID_CTX_KEY);
            }
        });
        rc.next();
    }
}
