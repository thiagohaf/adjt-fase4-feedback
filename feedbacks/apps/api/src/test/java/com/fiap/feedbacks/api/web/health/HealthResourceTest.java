package com.fiap.feedbacks.api.web.health;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class HealthResourceTest {

    @InjectSpy
    DataSource dataSource;

    @Test
    @DisplayName("FR-13 — GET /api/v1/health UP com DB ok")
    void healthUpQuandoDbOk() {
        given()
                .when()
                .get("/api/v1/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("database", equalTo("UP"));
    }

    @Test
    @DisplayName("FR-13 — SmallRye readiness UP com DB ok")
    void smallRyeReadyComDbOk() {
        given()
                .when()
                .get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("FR-13 — GET /api/v1/health DOWN quando conexão inválida")
    void healthDownQuandoConexaoInvalida() throws Exception {
        Connection invalid = mock(Connection.class);
        when(invalid.isValid(anyInt())).thenReturn(false);
        doReturn(invalid).when(dataSource).getConnection();

        given()
                .when()
                .get("/api/v1/health")
                .then()
                .statusCode(503)
                .body("status", equalTo("DOWN"))
                .body("database", equalTo("invalid"));
    }

    @Test
    @DisplayName("FR-13 — GET /api/v1/health DOWN quando SQLException")
    void healthDownQuandoSqlException() throws Exception {
        doThrow(new SQLException("db down")).when(dataSource).getConnection();

        given()
                .when()
                .get("/api/v1/health")
                .then()
                .statusCode(503)
                .body("status", equalTo("DOWN"))
                .body("database", equalTo("DOWN"));
    }
}
