package com.fiap.feedbacks.auth;

import com.fiap.feedbacks.support.JwtTestHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AuthTokenValidationTest {

    private static final UUID ESTUDANTE_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");

    @ConfigProperty(name = "app.jwt.secret")
    String jwtSecret;

    @Test
    @DisplayName("SPEC-1.7 — Acesso a rota protegida com token válido")
    void spec1_7_tokenValido() {
        String token = login("estudante@demo.fiap", "senha123");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/cursos")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("SPEC-1.8 — Acesso a rota protegida sem token")
    void spec1_8_semToken() {
        given()
                .when()
                .get("/api/v1/cursos")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTH_MISSING_TOKEN"))
                .body("traceId", notNullValue());
    }

    @Test
    @DisplayName("SPEC-1.9 — Token malformado / assinatura divergente")
    void spec1_9_tokenInvalido() {
        given()
                .header("Authorization", "Bearer token-invalido")
                .when()
                .get("/api/v1/cursos")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTH_INVALID_TOKEN"));

        String badSig = JwtTestHelper.createTokenWithInvalidSignature(ESTUDANTE_ID, "ESTUDANTE");
        given()
                .header("Authorization", "Bearer " + badSig)
                .when()
                .get("/api/v1/cursos")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTH_INVALID_TOKEN"));
    }

    @Test
    @DisplayName("SPEC-1.10 — Token expirado")
    void spec1_10_tokenExpirado() {
        String expired = JwtTestHelper.createExpiredToken(jwtSecret, ESTUDANTE_ID, "ESTUDANTE");

        given()
                .header("Authorization", "Bearer " + expired)
                .when()
                .get("/api/v1/cursos")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTH_TOKEN_EXPIRED"));
    }

    @Test
    @DisplayName("SPEC-1.11 — Login e health públicos")
    void spec1_11_rotasPublicas() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"estudante@demo.fiap","password":"errada1"}
                        """)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(401);

        given()
                .when()
                .get("/api/v1/health")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("traceId no envelope + propagação de X-Trace-Id")
    void traceIdPropagacao() {
        String customTrace = "trace-custom-12345";

        given()
                .header("X-Trace-Id", customTrace)
                .when()
                .get("/api/v1/cursos")
                .then()
                .statusCode(401)
                .header("X-Trace-Id", equalTo(customTrace))
                .body("traceId", equalTo(customTrace));
    }

    private String login(String email, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password))
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().path("accessToken");
    }
}
