package com.fiap.feedbacks.lambda.notification.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertPayloadParserTest {

    private AlertPayloadParser parser;

    @BeforeEach
    void setUp() {
        parser = new AlertPayloadParser(new ObjectMapper());
    }

    @Test
    void parseFixtureAd5Valida() throws IOException {
        String json = fixture("fixtures/ad5-alerta-alta.json");
        AlertPayload payload = parser.parse(json);
        assertThat(payload.urgencia()).isEqualTo("ALTA");
        assertThat(payload.descricao()).contains("Aula rápida");
        assertThat(payload.ocorridoEm()).isEqualTo("2026-07-22T14:30:00Z");
        assertThat(payload.avaliacaoId()).isEqualTo("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void parseJsonInvalidoFalha() {
        assertThatThrownBy(() -> parser.parse("{not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AD-5");
    }

    @Test
    void parseCampoAusenteFalha() {
        assertThatThrownBy(() -> parser.parse("{\"avaliacaoId\":\"x\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obrigatório");
    }

    @Test
    void parseBodyVazioFalha() {
        assertThatThrownBy(() -> parser.parse("  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseCampoNaoTextualFalha() {
        assertThatThrownBy(() -> parser.parse("""
                {"avaliacaoId":123,"descricao":"d","urgencia":"ALTA",
                "ocorridoEm":"t","aulaId":"a","cursoId":"c"}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("avaliacaoId");
    }

    @Test
    void parseCampoEmBrancoFalha() {
        assertThatThrownBy(() -> parser.parse("""
                {"avaliacaoId":"id","descricao":" ","urgencia":"ALTA",
                "ocorridoEm":"t","aulaId":"a","cursoId":"c"}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("descricao");
    }

    @Test
    void parseCampoNullJsonFalha() {
        assertThatThrownBy(() -> parser.parse("""
                {"avaliacaoId":"id","descricao":null,"urgencia":"ALTA",
                "ocorridoEm":"t","aulaId":"a","cursoId":"c"}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("descricao");
    }

    private static String fixture(String path) throws IOException {
        try (var in = AlertPayloadParserTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(in).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
