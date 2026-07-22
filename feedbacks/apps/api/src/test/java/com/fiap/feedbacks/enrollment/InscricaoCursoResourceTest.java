package com.fiap.feedbacks.enrollment;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class InscricaoCursoResourceTest {

    private static final String ESTUDANTE_ID = "a1111111-1111-4111-8111-111111111111";

    @Test
    @DisplayName("SPEC-5.1 — Inscrever em Curso com sucesso")
    void spec5_1_inscreverEmCursoComSucesso() {
        String adminToken = loginAdmin();
        String estudanteToken = loginEstudante();
        String cursoId = createCurso(adminToken, "Curso Inscricao " + UUID.randomUUID());

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .when()
                .post("/api/v1/cursos/" + cursoId + "/inscricoes")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("cursoId", equalTo(cursoId))
                .body("estudanteId", equalTo(ESTUDANTE_ID));
    }

    @Test
    @DisplayName("SPEC-5.2 — Inscrever em Curso inexistente")
    void spec5_2_cursoInexistente() {
        String estudanteToken = loginEstudante();

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .when()
                .post("/api/v1/cursos/" + UUID.randomUUID() + "/inscricoes")
                .then()
                .statusCode(404)
                .body("code", equalTo("CURSO_NOT_FOUND"));
    }

    @Test
    @DisplayName("SPEC-5.3 — Duplicata de inscrição em Curso")
    void spec5_3_duplicataCurso() {
        String adminToken = loginAdmin();
        String estudanteToken = loginEstudante();
        String cursoId = createCurso(adminToken, "Curso Duplicata " + UUID.randomUUID());

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .when()
                .post("/api/v1/cursos/" + cursoId + "/inscricoes")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .when()
                .post("/api/v1/cursos/" + cursoId + "/inscricoes")
                .then()
                .statusCode(409)
                .body("code", equalTo("INSCRICAO_DUPLICADA"));
    }

    @Test
    @DisplayName("SPEC-5.4 — Administrador não se inscreve em Curso")
    void spec5_4_adminNaoInscreve() {
        String adminToken = loginAdmin();
        String cursoId = createCurso(adminToken, "Curso Admin " + UUID.randomUUID());

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .post("/api/v1/cursos/" + cursoId + "/inscricoes")
                .then()
                .statusCode(403)
                .body("code", equalTo("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("SPEC-5.11 — Inscrição em Curso exige autenticação")
    void spec5_11_semToken() {
        given()
                .when()
                .post("/api/v1/cursos/" + UUID.randomUUID() + "/inscricoes")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTH_MISSING_TOKEN"));
    }

    private String createCurso(String token, String nome) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"%s\"}".formatted(nome))
                .when()
                .post("/api/v1/cursos")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private String loginAdmin() {
        return login("admin@demo.fiap", "admin123");
    }

    private String loginEstudante() {
        return login("estudante@demo.fiap", "senha123");
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
