package com.fiap.feedbacks.lambda.notification.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertPayloadTest {

    @Test
    void rejeitaCampoEmBranco() {
        assertThatThrownBy(() -> new AlertPayload("id", "desc", "ALTA", " ", "a", "c"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaNull() {
        assertThatThrownBy(() -> new AlertPayload(null, "d", "ALTA", "t", "a", "c"))
                .isInstanceOf(NullPointerException.class);
    }
}
