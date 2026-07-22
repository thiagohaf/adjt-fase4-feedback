package com.fiap.feedbacks.application.avaliacao;

import com.fiap.feedbacks.application.auth.port.CurrentUserProvider;
import com.fiap.feedbacks.domain.auth.Papel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ListarAvaliacoesUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final AvaliacaoReadRepository avaliacaoReadRepository;

    @Inject
    public ListarAvaliacoesUseCase(
            CurrentUserProvider currentUserProvider,
            AvaliacaoReadRepository avaliacaoReadRepository
    ) {
        this.currentUserProvider = currentUserProvider;
        this.avaliacaoReadRepository = avaliacaoReadRepository;
    }

    public List<Map<String, Object>> execute() {
        if (currentUserProvider.getCurrentRole() == Papel.ADMINISTRADOR) {
            return avaliacaoReadRepository.findAll();
        }
        return avaliacaoReadRepository.findByEstudanteId(currentUserProvider.getCurrentUserId());
    }
}
