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
class InscricaoAulaResourceTest {

    private static final String ESTUDANTE_ID = "a1111111-1111-4111-8111-111111111111";

    @Test
    @DisplayName("SPEC-5.5 — Inscrever em Aula com sucesso")
    void spec5_5_inscreverEmAulaComSucesso() {
        CatalogFixture fixture = createCatalogWithCursoEnrollment();

        given()
                .header("Authorization", "Bearer " + fixture.estudanteToken())
                .when()
                .post("/api/v1/cursos/" + fixture.cursoId() + "/aulas/" + fixture.aulaId() + "/inscricoes")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("cursoId", equalTo(fixture.cursoId()))
                .body("aulaId", equalTo(fixture.aulaId()))
                .body("estudanteId", equalTo(ESTUDANTE_ID));
    }

    @Test
    @DisplayName("SPEC-5.6 — Inscrever em Aula sem inscrição no Curso")
    void spec5_6_semInscricaoCurso() {
        String adminToken = loginAdmin();
        String estudanteToken = loginEstudante();
        String cursoId = createCurso(adminToken, "Curso Sem Inscricao " + UUID.randomUUID());
        String aulaId = createAula(adminToken, cursoId, "Aula Sem Inscricao");

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .when()
                .post("/api/v1/cursos/" + cursoId + "/aulas/" + aulaId + "/inscricoes")
                .then()
                .statusCode(409)
                .body("code", equalTo("INSCRICAO_CURSO_OBRIGATORIA"));
    }

    @Test
    @DisplayName("SPEC-5.7 — Duplicata de inscrição em Aula")
    void spec5_7_duplicataAula() {
        CatalogFixture fixture = createCatalogWithCursoEnrollment();

        given()
                .header("Authorization", "Bearer " + fixture.estudanteToken())
                .when()
                .post("/api/v1/cursos/" + fixture.cursoId() + "/aulas/" + fixture.aulaId() + "/inscricoes")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + fixture.estudanteToken())
                .when()
                .post("/api/v1/cursos/" + fixture.cursoId() + "/aulas/" + fixture.aulaId() + "/inscricoes")
                .then()
                .statusCode(409)
                .body("code", equalTo("INSCRICAO_DUPLICADA"));
    }

    @Test
    @DisplayName("SPEC-5.8 — Aula inexistente ou de outro Curso")
    void spec5_8_aulaInexistenteOuOutroCurso() {
        CatalogFixture fixture = createCatalogWithCursoEnrollment();
        String outroCursoId = createCurso(fixture.adminToken(), "Outro Curso " + UUID.randomUUID());
        String aulaOutroCurso = createAula(fixture.adminToken(), outroCursoId, "Aula Outro");

        given()
                .header("Authorization", "Bearer " + fixture.estudanteToken())
                .when()
                .post("/api/v1/cursos/" + fixture.cursoId() + "/aulas/" + UUID.randomUUID() + "/inscricoes")
                .then()
                .statusCode(404)
                .body("code", equalTo("AULA_NOT_FOUND"));

        given()
                .header("Authorization", "Bearer " + fixture.estudanteToken())
                .when()
                .post("/api/v1/cursos/" + fixture.cursoId() + "/aulas/" + aulaOutroCurso + "/inscricoes")
                .then()
                .statusCode(404)
                .body("code", equalTo("AULA_NOT_FOUND"));
    }

    @Test
    @DisplayName("SPEC-5.9 — Inscrever em Aula com Curso inexistente")
    void spec5_9_cursoInexistente() {
        String estudanteToken = loginEstudante();

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .when()
                .post("/api/v1/cursos/" + UUID.randomUUID() + "/aulas/" + UUID.randomUUID() + "/inscricoes")
                .then()
                .statusCode(404)
                .body("code", equalTo("CURSO_NOT_FOUND"));
    }

    @Test
    @DisplayName("SPEC-5.10 — Administrador não se inscreve em Aula")
    void spec5_10_adminNaoInscreve() {
        CatalogFixture fixture = createCatalogWithCursoEnrollment();

        given()
                .header("Authorization", "Bearer " + fixture.adminToken())
                .when()
                .post("/api/v1/cursos/" + fixture.cursoId() + "/aulas/" + fixture.aulaId() + "/inscricoes")
                .then()
                .statusCode(403)
                .body("code", equalTo("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("SPEC-5.11 — Inscrição em Aula exige autenticação")
    void spec5_11_semToken() {
        given()
                .when()
                .post("/api/v1/cursos/" + UUID.randomUUID() + "/aulas/" + UUID.randomUUID() + "/inscricoes")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTH_MISSING_TOKEN"));
    }

    private CatalogFixture createCatalogWithCursoEnrollment() {
        String adminToken = loginAdmin();
        String estudanteToken = loginEstudante();
        String cursoId = createCurso(adminToken, "Curso Aula " + UUID.randomUUID());
        String aulaId = createAula(adminToken, cursoId, "Aula Inscricao");

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .when()
                .post("/api/v1/cursos/" + cursoId + "/inscricoes")
                .then()
                .statusCode(201);

        return new CatalogFixture(adminToken, estudanteToken, cursoId, aulaId);
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

    private String createAula(String token, String cursoId, String nome) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"%s\"}".formatted(nome))
                .when()
                .post("/api/v1/cursos/" + cursoId + "/aulas")
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

    private record CatalogFixture(
            String adminToken,
            String estudanteToken,
            String cursoId,
            String aulaId
    ) {
    }
}
