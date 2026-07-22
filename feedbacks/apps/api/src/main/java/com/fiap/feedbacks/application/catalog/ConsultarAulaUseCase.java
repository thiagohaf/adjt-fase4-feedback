package com.fiap.feedbacks.application.catalog;

import com.fiap.feedbacks.application.catalog.port.AulaRepository;
import com.fiap.feedbacks.domain.catalog.Aula;
import com.fiap.feedbacks.domain.exception.AulaNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class ConsultarAulaUseCase {

    private final AulaRepository aulaRepository;

    @Inject
    public ConsultarAulaUseCase(AulaRepository aulaRepository) {
        this.aulaRepository = aulaRepository;
    }

    public Aula execute(UUID id) {
        return aulaRepository.findById(id)
                .orElseThrow(() -> new AulaNotFoundException(id));
    }
}
