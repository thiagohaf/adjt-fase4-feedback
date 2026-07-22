package com.fiap.feedbacks.catalog;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class CursoCatalogResourceTest {

    @Test
    @DisplayName("SPEC-3.1 — Criar Curso com sucesso")
    void spec3_1_criarCursoComSucesso() {
        String token = loginAdmin();

        String cursoId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Arquitetura Cloud\",\"descricao\":\"Curso introdutório\"}")
                .when()
                .post("/api/v1/cursos")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("nome", equalTo("Arquitetura Cloud"))
                .body("descricao", equalTo("Curso introdutório"))
                .extract().path("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/cursos/" + cursoId)
                .then()
                .statusCode(200)
                .body("id", equalTo(cursoId))
                .body("nome", equalTo("Arquitetura Cloud"));

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/cursos")
                .then()
                .statusCode(200)
                .body("find { it.id == '%s' }.nome".formatted(cursoId), equalTo("Arquitetura Cloud"));
    }

    @Test
    @DisplayName("SPEC-3.2 — Criar Curso só com nome")
    void spec3_2_criarCursoSomenteNome() {
        String token = loginAdmin();

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Somente Nome\"}")
                .when()
                .post("/api/v1/cursos")
                .then()
                .statusCode(201)
                .body("nome", equalTo("Somente Nome"))
                .body("descricao", nullValue());
    }

    @Test
    @DisplayName("SPEC-3.3 — Criar Curso sem nome")
    void spec3_3_criarCursoSemNome() {
        String token = loginAdmin();

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"   \"}")
                .when()
                .post("/api/v1/cursos")
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("SPEC-3.4 — Estudante não cria Curso")
    void spec3_4_estudanteNaoCriaCurso() {
        String token = loginEstudante();

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
    @DisplayName("SPEC-4.1 — Listar Cursos")
    void spec4_1_listarCursos() {
        String token = loginEstudante();
        createCurso(loginAdmin(), "Curso Lista");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/cursos")
                .then()
                .statusCode(200)
                .body("[0].id", notNullValue())
                .body("[0].nome", notNullValue());
    }

    @Test
    @DisplayName("SPEC-4.2 — Consultar Curso por id")
    void spec4_2_consultarCursoPorId() {
        String token = loginAdmin();
        String cursoId = createCurso(token, "Curso Consulta");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/cursos/" + cursoId)
                .then()
                .statusCode(200)
                .body("id", equalTo(cursoId))
                .body("nome", equalTo("Curso Consulta"))
                .body("descricao", nullValue());
    }

    @Test
    @DisplayName("SPEC-4.3 — Consultar Curso inexistente")
    void spec4_3_consultarCursoInexistente() {
        String token = loginAdmin();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/cursos/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("code", equalTo("CURSO_NOT_FOUND"));
    }

    @Test
    @DisplayName("SPEC-4.8 — Catálogo Curso sem token")
    void spec4_8_catalogoCursoSemToken() {
        given()
                .when()
                .get("/api/v1/cursos")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTH_MISSING_TOKEN"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"X\"}")
                .when()
                .post("/api/v1/cursos")
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
