package com.fiap.feedbacks.api.web.error;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@ApplicationScoped
public final class AuthExceptionMappers {

    private AuthExceptionMappers() {
    }

    @Provider
    @ApplicationScoped
    public static class UnauthorizedExceptionMapper implements ExceptionMapper<UnauthorizedException> {

        @Override
        public Response toResponse(UnauthorizedException exception) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiErrorFactory.of("AUTH_MISSING_TOKEN", "Missing authentication token"))
                    .build();
        }
    }

    @Provider
    @ApplicationScoped
    public static class AuthenticationFailedExceptionMapper
            implements ExceptionMapper<AuthenticationFailedException> {

        @Inject
        RoutingContext routingContext;

        @Override
        public Response toResponse(AuthenticationFailedException exception) {
            if (isExpired(exception) || isExpiredBearer(routingContext)) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(ApiErrorFactory.of("AUTH_TOKEN_EXPIRED", "Token expired"))
                        .build();
            }
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiErrorFactory.of("AUTH_INVALID_TOKEN", "Invalid token"))
                    .build();
        }

        private static boolean isExpired(AuthenticationFailedException exception) {
            Throwable cause = exception;
            while (cause != null) {
                String message = cause.getMessage();
                String name = cause.getClass().getName().toLowerCase();
                if (message != null) {
                    String lower = message.toLowerCase();
                    if (lower.contains("expir") || lower.contains("the jwt is no longer valid")) {
                        return true;
                    }
                }
                if (name.contains("expired")) {
                    return true;
                }
                cause = cause.getCause();
            }
            return false;
        }

        private static boolean isExpiredBearer(RoutingContext routingContext) {
            if (routingContext == null || routingContext.request() == null) {
                return false;
            }
            String auth = routingContext.request().getHeader("Authorization");
            if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return false;
            }
            String token = auth.substring(7).trim();
            return payloadExpInPast(token);
        }

        /**
         * Fallback leve (design risks): inspeciona claim {@code exp} no payload JWT
         * sem revalidar assinatura — apenas para classificar AUTH_TOKEN_EXPIRED.
         */
        static boolean payloadExpInPast(String token) {
            if (token == null || token.isBlank()) {
                return false;
            }
            int dot1 = token.indexOf('.');
            int dot2 = token.indexOf('.', dot1 + 1);
            if (dot1 <= 0 || dot2 <= dot1) {
                return false;
            }
            try {
                String payloadJson = new String(
                        java.util.Base64.getUrlDecoder().decode(token.substring(dot1 + 1, dot2)),
                        java.nio.charset.StandardCharsets.UTF_8
                );
                int expIdx = payloadJson.indexOf("\"exp\"");
                if (expIdx < 0) {
                    return false;
                }
                int colon = payloadJson.indexOf(':', expIdx);
                int i = colon + 1;
                while (i < payloadJson.length() && !Character.isDigit(payloadJson.charAt(i))) {
                    i++;
                }
                int j = i;
                while (j < payloadJson.length() && Character.isDigit(payloadJson.charAt(j))) {
                    j++;
                }
                if (i >= j) {
                    return false;
                }
                long exp = Long.parseLong(payloadJson.substring(i, j));
                return exp < (System.currentTimeMillis() / 1000L);
            } catch (RuntimeException ignored) {
                return false;
            }
        }
    }

    @Provider
    @ApplicationScoped
    public static class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {

        @Override
        public Response toResponse(ForbiddenException exception) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiErrorFactory.of("AUTH_FORBIDDEN", "Access forbidden"))
                    .build();
        }
    }
}
