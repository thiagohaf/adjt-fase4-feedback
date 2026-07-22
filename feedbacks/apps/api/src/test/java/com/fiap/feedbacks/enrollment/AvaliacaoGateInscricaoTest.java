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
class AvaliacaoGateInscricaoTest {

    @Test
    @DisplayName("SPEC-6.1 — Criar Avaliação sem inscrição na Aula é rejeitado")
    void spec6_1_semInscricaoRejeita() {
        String adminToken = loginAdmin();
        String estudanteToken = loginEstudante();
        String cursoId = createCurso(adminToken, "Curso Gate " + UUID.randomUUID());
        String aulaId = createAula(adminToken, cursoId, "Aula Gate");

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .contentType(ContentType.JSON)
                .body("{\"aulaId\":\"%s\",\"descricao\":\"Feedback\",\"nota\":5}".formatted(aulaId))
                .when()
                .post("/api/v1/avaliacoes")
                .then()
                .statusCode(403)
                .body("code", equalTo("INSCRICAO_AULA_OBRIGATORIA"));
    }

    @Test
    @DisplayName("SPEC-6.2 — Criar Avaliação com inscrição na Aula passa o gate")
    void spec6_2_comInscricaoPassaGate() {
        String adminToken = loginAdmin();
        String estudanteToken = loginEstudante();
        String cursoId = createCurso(adminToken, "Curso Gate Ok " + UUID.randomUUID());
        String aulaId = createAula(adminToken, cursoId, "Aula Gate Ok");

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .when()
                .post("/api/v1/cursos/" + cursoId + "/inscricoes")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .when()
                .post("/api/v1/cursos/" + cursoId + "/aulas/" + aulaId + "/inscricoes")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + estudanteToken)
                .contentType(ContentType.JSON)
                .body("{\"aulaId\":\"%s\",\"descricao\":\"Feedback\",\"nota\":4}".formatted(aulaId))
                .when()
                .post("/api/v1/avaliacoes")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("aulaId", equalTo(aulaId))
                .body("cursoId", equalTo(cursoId))
                .body("descricao", equalTo("Feedback"))
                .body("nota", equalTo(4))
                .body("urgencia", equalTo("ALTA"))
                .body("ocorridoEm", notNullValue());
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
}
