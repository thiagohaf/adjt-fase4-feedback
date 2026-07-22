package com.fiap.feedbacks.infrastructure.persistence;

import com.fiap.feedbacks.domain.auth.Papel;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntityUnitTest {

    @Test
    void usuarioEntity_getters() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        var entity = new UsuarioEntity(id, "a@b.c", "hash", Papel.ESTUDANTE, now);
        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getEmail()).isEqualTo("a@b.c");
        assertThat(entity.getSenhaHash()).isEqualTo("hash");
        assertThat(entity.getPapel()).isEqualTo(Papel.ESTUDANTE);
        assertThat(entity.getCriadoEm()).isEqualTo(now);
    }

    @Test
    void avaliacaoEntity_getters() {
        UUID id = UUID.randomUUID();
        UUID estudanteId = UUID.randomUUID();
        var entity = new AvaliacaoEntity(id, estudanteId, "desc");
        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getEstudanteId()).isEqualTo(estudanteId);
        assertThat(entity.getDescricao()).isEqualTo("desc");
    }
}
