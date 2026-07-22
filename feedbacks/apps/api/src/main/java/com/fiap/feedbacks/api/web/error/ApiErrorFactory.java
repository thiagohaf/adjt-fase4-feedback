package com.fiap.feedbacks.api.web.error;

import io.vertx.core.Vertx;
import org.jboss.logging.MDC;

final class ApiErrorFactory {

    private ApiErrorFactory() {
    }

    static ApiErrorResponse of(String code, String message) {
        String traceId = resolveTraceId();
        return new ApiErrorResponse(code, message, traceId);
    }

    private static String resolveTraceId() {
        Object fromMdc = MDC.get(TraceIdFilterRegistrar.TRACE_ID_MDC_KEY);
        if (fromMdc != null) {
            return fromMdc.toString();
        }
        if (Vertx.currentContext() != null) {
            Object fromCtx = Vertx.currentContext().getLocal(TraceIdFilterRegistrar.TRACE_ID_CTX_KEY);
            if (fromCtx != null) {
                return fromCtx.toString();
            }
        }
        return null;
    }
}
