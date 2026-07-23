package com.fiap.feedbacks.lambda.notification.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Parse e validação de JSON UTF-8 no contrato AD-5.
 */
@ApplicationScoped
public class AlertPayloadParser {

    private final ObjectMapper objectMapper;

    @Inject
    public AlertPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AlertPayload parse(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Body AD-5 vazio");
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            return new AlertPayload(
                    requiredText(node, "avaliacaoId"),
                    requiredText(node, "descricao"),
                    requiredText(node, "urgencia"),
                    requiredText(node, "ocorridoEm"),
                    requiredText(node, "aulaId"),
                    requiredText(node, "cursoId")
            );
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Body não é JSON AD-5 válido", ex);
        }
    }

    private static String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Campo AD-5 obrigatório ausente ou inválido: " + field);
        }
        return value.asText();
    }
}
