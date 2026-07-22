package com.fiap.feedbacks.auth;

import com.fiap.feedbacks.support.JwtClaimsReader;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AuthLoginResourceTest {

    private static final UUID ESTUDANTE_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");

    @ConfigProperty(name = "app.jwt.secret")
    String jwtSecret;

    @Test
    @DisplayName("SPEC-1.1 — Login bem-sucedido (Estudante)")
    void spec1_1_loginEstudante() throws Exception {
        String token = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"estudante@demo.fiap","password":"senha123"}
                        """)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .body("expiresIn", notNullValue())
                .extract().path("accessToken");

        var claims = JwtClaimsReader.readClaims(token, jwtSecret);
        assertThat(claims.getSubject()).isEqualTo(ESTUDANTE_ID.toString());
        assertThat(claims.getStringClaimValue("role")).isEqualTo("ESTUDANTE");
        assertThat(claims.getExpirationTime().getValue())
                .isGreaterThan(System.currentTimeMillis() / 1000);
    }

    @Test
    @DisplayName("SPEC-1.2 — Login bem-sucedido (Administrador)")
    void spec1_2_loginAdmin() throws Exception {
        String token = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"admin@demo.fiap","password":"admin123"}
                        """)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().path("accessToken");

        var claims = JwtClaimsReader.readClaims(token, jwtSecret);
        assertThat(claims.getStringClaimValue("role")).isEqualTo("ADMINISTRADOR");
    }

    @Test
    @DisplayName("SPEC-1.3 — Credenciais inválidas (email inexistente)")
    void spec1_3_emailInexistente() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"naoexiste@demo.fiap","password":"qualquer"}
                        """)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTH_INVALID_CREDENTIALS"))
                .body("message", notNullValue())
                .body("traceId", notNullValue());
    }

    @Test
    @DisplayName("SPEC-1.4 — Credenciais inválidas (senha incorreta)")
    void spec1_4_senhaIncorreta() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"estudante@demo.fiap","password":"errada1"}
                        """)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("SPEC-1.5 — Validação: email ausente/inválido")
    void spec1_5_emailInvalido() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"nao-e-email","password":"senha123"}
                        """)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("SPEC-1.6 — Validação: password ausente/curto")
    void spec1_6_passwordAusente() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"email":"estudante@demo.fiap"}
                        """)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }
}
