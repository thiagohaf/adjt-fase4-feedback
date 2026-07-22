package com.fiap.feedbacks.api.web.avaliacao;

import com.fiap.feedbacks.api.web.avaliacao.dto.CriarAvaliacaoStubRequest;
import com.fiap.feedbacks.application.auth.port.CurrentUserProvider;
import com.fiap.feedbacks.application.avaliacao.ListarAvaliacoesUseCase;
import com.fiap.feedbacks.application.enrollment.VerificarInscricaoAulaUseCase;
import com.fiap.feedbacks.domain.exception.InscricaoAulaObrigatoriaException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GET: read model mínimo (SPEC-2.10/2.11).
 * POST: stub provisório com gate FR-6 (inscrição na Aula obrigatória).
 */
@Path("/api/v1/avaliacoes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AvaliacaoResource {

    @Inject
    ListarAvaliacoesUseCase listarAvaliacoesUseCase;
    @Inject
    VerificarInscricaoAulaUseCase verificarInscricaoAulaUseCase;
    @Inject
    CurrentUserProvider currentUserProvider;

    @GET
    @RolesAllowed({"ESTUDANTE", "ADMINISTRADOR"})
    public List<Map<String, Object>> listar() {
        return listarAvaliacoesUseCase.execute();
    }

    @POST
    @RolesAllowed("ESTUDANTE")
    public Response criar(@Valid CriarAvaliacaoStubRequest request) {
        UUID estudanteId = currentUserProvider.getCurrentUserId();
        if (!verificarInscricaoAulaUseCase.execute(estudanteId, request.aulaId())) {
            throw new InscricaoAulaObrigatoriaException(request.aulaId());
        }

        String descricao = request.descricao() == null || request.descricao().isBlank()
                ? "Feedback demo"
                : request.descricao();
        return Response.status(Response.Status.CREATED)
                .entity(Map.of(
                        "id", UUID.randomUUID(),
                        "aulaId", request.aulaId(),
                        "descricao", descricao
                ))
                .build();
    }
}
