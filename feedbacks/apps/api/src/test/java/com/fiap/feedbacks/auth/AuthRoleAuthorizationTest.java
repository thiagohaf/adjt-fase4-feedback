package com.fiap.feedbacks.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class AuthRoleAuthorizationTest {

    private static final UUID ESTUDANTE_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");

    @Test
    @DisplayName("SPEC-2.1 — Estudante não cria Curso")
    void spec2_1_estudanteNaoCriaCurso() {
        String token = login("estudante@demo.fiap", "senha123");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Novo Curso\"}")
                .when()
                .post("/api/v1/cursos")
                .then()
                .statusCode(403)
                .body("code", equalTo("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("SPEC-2.2 — Estudante não cria Aula")
    void spec2_2_estudanteNaoCriaAula() {
        String token = login("estudante@demo.fiap", "senha123");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"titulo\":\"Nova Aula\"}")
                .when()
                .post("/api/v1/cursos/" + UUID.randomUUID() + "/aulas")
                .then()
                .statusCode(403)
                .body("code", equalTo("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("SPEC-2.3 — Administrador cria Curso e Aula")
    void spec2_3_adminCriaCursoEAula() {
        String token = login("admin@demo.fiap", "admin123");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Novo Curso\"}")
                .when()
                .post("/api/v1/cursos")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"titulo\":\"Nova Aula\"}")
                .when()
                .post("/api/v1/cursos/" + UUID.randomUUID() + "/aulas")
                .then()
                .statusCode(201);
    }

    @Test
    @DisplayName("SPEC-2.4 — Administrador consulta catálogo")
    void spec2_4_adminConsultaCatalogo() {
        String token = login("admin@demo.fiap", "admin123");
        UUID cursoId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/cursos")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/cursos/" + cursoId + "/aulas")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("SPEC-2.5 — Estudante consulta catálogo")
    void spec2_5_estudanteConsultaCatalogo() {
        String token = login("estudante@demo.fiap", "senha123");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/cursos")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/aulas/" + UUID.randomUUID())
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("SPEC-2.6 — Admin não cria Avaliação")
    void spec2_6_adminNaoCriaAvaliacao() {
        String token = login("admin@demo.fiap", "admin123");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"descricao\":\"Feedback\"}")
                .when()
                .post("/api/v1/avaliacoes")
                .then()
                .statusCode(403)
                .body("code", equalTo("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("SPEC-2.7 — Estudante cria Avaliação")
    void spec2_7_estudanteCriaAvaliacao() {
        String token = login("estudante@demo.fiap", "senha123");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"descricao\":\"Feedback\"}")
                .when()
                .post("/api/v1/avaliacoes")
                .then()
                .statusCode(201);
    }

    @Test
    @DisplayName("SPEC-2.8 — Inscrição exige papel Estudante")
    void spec2_8_adminNaoInscreve() {
        String token = login("admin@demo.fiap", "admin123");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/api/v1/cursos/" + UUID.randomUUID() + "/inscricoes")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("SPEC-2.9 — Estudante realiza inscrição")
    void spec2_9_estudanteInscreve() {
        String token = login("estudante@demo.fiap", "senha123");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/api/v1/cursos/" + UUID.randomUUID() + "/inscricoes")
                .then()
                .statusCode(201);
    }

    @Test
    @DisplayName("SPEC-2.10 — Admin vê todas Avaliações")
    void spec2_10_adminVeTodas() {
        String token = login("admin@demo.fiap", "admin123");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/avaliacoes")
                .then()
                .statusCode(200)
                .body("$", hasSize(2));
    }

    @Test
    @DisplayName("SPEC-2.11 — Estudante vê só as próprias Avaliações")
    void spec2_11_estudanteVeProprias() {
        String token = login("estudante@demo.fiap", "senha123");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/avaliacoes")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].estudanteId", equalTo(ESTUDANTE_ID.toString()));
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
