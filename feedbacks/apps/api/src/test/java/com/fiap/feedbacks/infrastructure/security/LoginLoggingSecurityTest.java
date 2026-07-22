package com.fiap.feedbacks.infrastructure.security;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class LoginLoggingSecurityTest {

    private CapturingHandler handler;
    private Logger logger;

    @BeforeEach
    void setUp() {
        handler = new CapturingHandler();
        logger = Logger.getLogger("com.fiap.feedbacks.api.web.auth.AuthResource");
        logger.addHandler(handler);
        logger.setLevel(java.util.logging.Level.INFO);
    }

    @AfterEach
    void tearDown() {
        if (logger != null && handler != null) {
            logger.removeHandler(handler);
        }
    }

    @Test
    @DisplayName("SPEC-NFR-2 — log de login sem password nem JWT completo")
    void specNfr2_logSemSegredos() {
        String responseToken = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"estudante@demo.fiap","password":"senha123"}
                        """)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().path("accessToken");

        String allLogs = String.join("\n", handler.messages);
        assertThat(allLogs).contains("estudante@demo.fiap");
        assertThat(allLogs).doesNotContain("senha123");
        assertThat(allLogs).doesNotContain("password");
        assertThat(allLogs).doesNotContain(responseToken);
    }

    static final class CapturingHandler extends Handler {
        final List<String> messages = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (record.getMessage() != null) {
                messages.add(record.getMessage());
                if (record.getParameters() != null) {
                    messages.add(java.text.MessageFormat.format(record.getMessage(), record.getParameters()));
                }
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
