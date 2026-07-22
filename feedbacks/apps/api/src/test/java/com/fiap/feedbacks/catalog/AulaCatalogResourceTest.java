package com.fiap.feedbacks.catalog;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class AulaCatalogResourceTest {

    @Test
    @DisplayName("SPEC-3.5 — Criar Aula com sucesso")
    void spec3_5_criarAulaComSucesso() {
        String token = loginAdmin();
        String cursoId = createCurso(token);

        String aulaId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Aula 1 — Containers\",\"descricao\":\"Docker e Kubernetes\"}")
                .when()
                .post("/api/v1/cursos/" + cursoId + "/aulas")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("cursoId", equalTo(cursoId))
                .body("nome", equalTo("Aula 1 — Containers"))
                .body("descricao", equalTo("Docker e Kubernetes"))
                .body("$", not(hasKey("titulo")))
                .extract().path("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/cursos/" + cursoId + "/aulas")
                .then()
                .statusCode(200)
                .body("find { it.id == '%s' }.nome".formatted(aulaId), equalTo("Aula 1 — Containers"));

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/aulas/" + aulaId)
                .then()
                .statusCode(200)
                .body("nome", equalTo("Aula 1 — Containers"))
                .body("$", not(hasKey("titulo")));
    }

    @Test
    @DisplayName("SPEC-3.6 — Criar Aula só com nome")
    void spec3_6_criarAulaSomenteNome() {
        String token = loginAdmin();
        String cursoId = createCurso(token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Aula mínima\"}")
                .when()
                .post("/api/v1/cursos/" + cursoId + "/aulas")
                .then()
                .statusCode(201)
                .body("nome", equalTo("Aula mínima"))
                .body("descricao", nullValue());
    }

    @Test
    @DisplayName("SPEC-3.7 — Criar Aula sem nome")
    void spec3_7_criarAulaSemNome() {
        String token = loginAdmin();
        String cursoId = createCurso(token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/v1/cursos/" + cursoId + "/aulas")
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("SPEC-3.8 — Criar Aula com Curso inexistente")
    void spec3_8_criarAulaCursoInexistente() {
        String token = loginAdmin();

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Aula órfã\"}")
                .when()
                .post("/api/v1/cursos/" + UUID.randomUUID() + "/aulas")
                .then()
                .statusCode(404)
                .body("code", equalTo("CURSO_NOT_FOUND"));
    }

    @Test
    @DisplayName("SPEC-3.9 — Estudante não cria Aula")
    void spec3_9_estudanteNaoCriaAula() {
        String adminToken = loginAdmin();
        String cursoId = createCurso(adminToken);
        String estudanteToken = loginEstudante();

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Nova Aula\"}")
                .when()
                .post("/api/v1/cursos/" + cursoId + "/aulas")
                .then()
                .statusCode(403)
                .body("code", equalTo("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("SPEC-4.4 — Listar Aulas de um Curso")
    void spec4_4_listarAulasDoCurso() {
        String token = loginAdmin();
        String cursoId = createCurso(token);
        createAula(token, cursoId, "Aula Lista");

        given()
                .header("Authorization", "Bearer " + loginEstudante())
                .when()
                .get("/api/v1/cursos/" + cursoId + "/aulas")
                .then()
                .statusCode(200)
                .body("[0].id", notNullValue())
                .body("[0].cursoId", equalTo(cursoId))
                .body("[0].nome", equalTo("Aula Lista"));
    }

    @Test
    @DisplayName("SPEC-4.5 — Listar Aulas de Curso inexistente")
    void spec4_5_listarAulasCursoInexistente() {
        String token = loginEstudante();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/cursos/" + UUID.randomUUID() + "/aulas")
                .then()
                .statusCode(404)
                .body("code", equalTo("CURSO_NOT_FOUND"));
    }

    @Test
    @DisplayName("SPEC-4.6 — Consultar Aula por id")
    void spec4_6_consultarAulaPorId() {
        String token = loginAdmin();
        String cursoId = createCurso(token);
        String aulaId = createAula(token, cursoId, "Aula Consulta");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/aulas/" + aulaId)
                .then()
                .statusCode(200)
                .body("id", equalTo(aulaId))
                .body("cursoId", equalTo(cursoId))
                .body("nome", equalTo("Aula Consulta"))
                .body("descricao", nullValue())
                .body("$", not(hasKey("titulo")));
    }

    @Test
    @DisplayName("SPEC-4.7 — Consultar Aula inexistente")
    void spec4_7_consultarAulaInexistente() {
        String token = loginAdmin();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/v1/aulas/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("code", equalTo("AULA_NOT_FOUND"));
    }

    @Test
    @DisplayName("SPEC-4.8 — Catálogo Aula sem token")
    void spec4_8_catalogoAulaSemToken() {
        given()
                .when()
                .get("/api/v1/aulas/" + UUID.randomUUID())
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTH_MISSING_TOKEN"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"X\"}")
                .when()
                .post("/api/v1/cursos/" + UUID.randomUUID() + "/aulas")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTH_MISSING_TOKEN"));
    }

    private String createCurso(String token) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"nome\":\"Curso para Aulas\"}")
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
}
