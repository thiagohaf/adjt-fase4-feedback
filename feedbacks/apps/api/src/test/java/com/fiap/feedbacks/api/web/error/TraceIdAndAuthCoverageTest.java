package com.fiap.feedbacks.api.web.error;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceIdAndAuthCoverageTest {

    @Mock
    ContainerRequestContext requestContext;

    @Mock
    ContainerResponseContext responseContext;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void apiErrorFactory_usaTraceIdDoMdc() {
        MDC.put(TraceIdFilterRegistrar.TRACE_ID_MDC_KEY, "trace-from-mdc");
        ApiErrorResponse err = ApiErrorFactory.of("X", "msg");
        assertThat(err.traceId()).isEqualTo("trace-from-mdc");
    }

    @Test
    void apiErrorFactory_semMdcRetornaNull() {
        ApiErrorResponse err = ApiErrorFactory.of("X", "msg");
        assertThat(err.traceId()).isNull();
    }

    @Test
    void traceIdFilter_geraOuReusaHeader() {
        when(requestContext.getHeaderString(TraceIdFilter.TRACE_ID_HEADER)).thenReturn(null);
        TraceIdFilter filter = new TraceIdFilter();
        filter.filter(requestContext);
        verify(requestContext).setProperty(anyString(), anyString());
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNotNull();
    }

    @Test
    void traceIdFilter_respeitaHeaderExistente() {
        when(requestContext.getHeaderString(TraceIdFilter.TRACE_ID_HEADER)).thenReturn("client-trace");
        TraceIdFilter filter = new TraceIdFilter();
        filter.filter(requestContext);
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isEqualTo("client-trace");
    }

    @Test
    void traceIdFilter_responsePropagaHeader() throws Exception {
        when(requestContext.getProperty(TraceIdFilter.TRACE_ID_MDC_KEY)).thenReturn("resp-trace");
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(headers);

        new TraceIdFilter().filter(requestContext, responseContext);

        assertThat(headers.getFirst(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo("resp-trace");
    }

    @Test
    void traceIdFilter_responseNaoSobrescreveHeader() throws Exception {
        when(requestContext.getProperty(TraceIdFilter.TRACE_ID_MDC_KEY)).thenReturn("resp-trace");
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(TraceIdFilter.TRACE_ID_HEADER, "already");
        when(responseContext.getHeaders()).thenReturn(headers);

        new TraceIdFilter().filter(requestContext, responseContext);

        assertThat(headers.getFirst(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo("already");
    }

    @Test
    void traceIdFilterRegistrar_handleComHeaderESemHeader() throws Exception {
        TraceIdFilterRegistrar registrar = new TraceIdFilterRegistrar();
        Method handle = TraceIdFilterRegistrar.class.getDeclaredMethod("handle", RoutingContext.class);
        handle.setAccessible(true);

        RoutingContext rc = mock(RoutingContext.class);
        HttpServerRequest req = mock(HttpServerRequest.class);
        io.vertx.core.http.HttpServerResponse res = mock(io.vertx.core.http.HttpServerResponse.class);
        when(rc.request()).thenReturn(req);
        when(rc.response()).thenReturn(res);
        when(res.putHeader(anyString(), anyString())).thenReturn(res);
        when(req.getHeader(TraceIdFilterRegistrar.TRACE_ID_HEADER)).thenReturn("vert-x-trace");

        handle.invoke(registrar, rc);
        verify(rc).next();
        verify(res).putHeader(TraceIdFilterRegistrar.TRACE_ID_HEADER, "vert-x-trace");

        when(req.getHeader(TraceIdFilterRegistrar.TRACE_ID_HEADER)).thenReturn("  ");
        handle.invoke(registrar, rc);
        verify(rc, org.mockito.Mockito.times(2)).next();
    }

    @Test
    void authFailed_expiredByClassName() {
        class ExpiredJwtBoom extends RuntimeException {
            ExpiredJwtBoom() {
                super("x");
            }
        }
        var mapper = new AuthExceptionMappers.AuthenticationFailedExceptionMapper();
        var response = mapper.toResponse(
                new io.quarkus.security.AuthenticationFailedException("fail", new ExpiredJwtBoom()));
        assertThat(((ApiErrorResponse) response.getEntity()).code()).isEqualTo("AUTH_TOKEN_EXPIRED");
    }

    @Test
    void authFailed_expiredViaBearerFallback() {
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"exp\":1}".getBytes());
        String token = "h." + payload + ".s";

        RoutingContext rc = mock(RoutingContext.class);
        HttpServerRequest req = mock(HttpServerRequest.class);
        when(rc.request()).thenReturn(req);
        when(req.getHeader("Authorization")).thenReturn("Bearer " + token);

        var mapper = new AuthExceptionMappers.AuthenticationFailedExceptionMapper();
        mapper.routingContext = rc;

        var response = mapper.toResponse(new io.quarkus.security.AuthenticationFailedException("bad"));
        assertThat(((ApiErrorResponse) response.getEntity()).code()).isEqualTo("AUTH_TOKEN_EXPIRED");
    }

    @Test
    void traceIdFilter_responseSemTraceIdNaoAdicionaHeader() throws Exception {
        when(requestContext.getProperty(TraceIdFilter.TRACE_ID_MDC_KEY)).thenReturn(null);

        new TraceIdFilter().filter(requestContext, responseContext);

        // when property and MDC are null, headers are never touched
        org.mockito.Mockito.verify(responseContext, org.mockito.Mockito.never()).getHeaders();
    }

    @Test
    void authFailed_bearerSemPrefixoMantemInvalidToken() {
        RoutingContext rc = mock(RoutingContext.class);
        HttpServerRequest req = mock(HttpServerRequest.class);
        when(rc.request()).thenReturn(req);
        when(req.getHeader("Authorization")).thenReturn("Basic abc");

        var mapper = new AuthExceptionMappers.AuthenticationFailedExceptionMapper();
        mapper.routingContext = rc;
        var response = mapper.toResponse(new io.quarkus.security.AuthenticationFailedException("bad"));
        assertThat(((ApiErrorResponse) response.getEntity()).code()).isEqualTo("AUTH_INVALID_TOKEN");
    }

    @Test
    void authFailed_bearerComTokenBlankMantemInvalidToken() {
        RoutingContext rc = mock(RoutingContext.class);
        HttpServerRequest req = mock(HttpServerRequest.class);
        when(rc.request()).thenReturn(req);
        when(req.getHeader("Authorization")).thenReturn("Bearer    ");

        var mapper = new AuthExceptionMappers.AuthenticationFailedExceptionMapper();
        mapper.routingContext = rc;
        var response = mapper.toResponse(new io.quarkus.security.AuthenticationFailedException("bad"));
        assertThat(((ApiErrorResponse) response.getEntity()).code()).isEqualTo("AUTH_INVALID_TOKEN");
    }

    @Test
    void payloadExpInPast_cobreBordas() {
        assertThat(AuthExceptionMappers.AuthenticationFailedExceptionMapper.payloadExpInPast("")).isFalse();
        assertThat(AuthExceptionMappers.AuthenticationFailedExceptionMapper.payloadExpInPast("   ")).isFalse();
        assertThat(AuthExceptionMappers.AuthenticationFailedExceptionMapper.payloadExpInPast("a.b")).isFalse();
        assertThat(AuthExceptionMappers.AuthenticationFailedExceptionMapper.payloadExpInPast("..sig")).isFalse();

        String future = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"exp\":" + (System.currentTimeMillis() / 1000L + 3600) + "}").getBytes());
        assertThat(AuthExceptionMappers.AuthenticationFailedExceptionMapper.payloadExpInPast("h." + future + ".s")).isFalse();

        String noExp = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"x\"}".getBytes());
        assertThat(AuthExceptionMappers.AuthenticationFailedExceptionMapper.payloadExpInPast("h." + noExp + ".s")).isFalse();

        String bad = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"exp\":".getBytes());
        assertThat(AuthExceptionMappers.AuthenticationFailedExceptionMapper.payloadExpInPast("h." + bad + ".s")).isFalse();

        String spaced = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"exp\" : 1}".getBytes());
        assertThat(AuthExceptionMappers.AuthenticationFailedExceptionMapper.payloadExpInPast("h." + spaced + ".s")).isTrue();
    }

    @Test
    void traceIdFilter_usaMdcExistenteQuandoHeaderBlank() {
        MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, "from-mdc");
        when(requestContext.getHeaderString(TraceIdFilter.TRACE_ID_HEADER)).thenReturn("  ");
        new TraceIdFilter().filter(requestContext);
        verify(requestContext).setProperty(TraceIdFilter.TRACE_ID_MDC_KEY, "from-mdc");
    }
}
