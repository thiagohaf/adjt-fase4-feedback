package com.fiap.feedbacks.application.enrollment;

import com.fiap.feedbacks.application.enrollment.port.InscricaoAulaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class VerificarInscricaoAulaUseCase {

    private final InscricaoAulaRepository inscricaoAulaRepository;

    @Inject
    public VerificarInscricaoAulaUseCase(InscricaoAulaRepository inscricaoAulaRepository) {
        this.inscricaoAulaRepository = inscricaoAulaRepository;
    }

    public boolean execute(UUID estudanteId, UUID aulaId) {
        return inscricaoAulaRepository.existsByEstudanteIdAndAulaId(estudanteId, aulaId);
    }
}
