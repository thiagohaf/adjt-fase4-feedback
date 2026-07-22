package com.fiap.feedbacks.api.web.catalog;

import com.fiap.feedbacks.api.web.catalog.dto.AulaResponse;
import com.fiap.feedbacks.api.web.catalog.dto.CriarAulaRequest;
import com.fiap.feedbacks.api.web.catalog.dto.CriarCursoRequest;
import com.fiap.feedbacks.api.web.catalog.dto.CursoResponse;
import com.fiap.feedbacks.api.web.enrollment.dto.InscricaoAulaResponse;
import com.fiap.feedbacks.api.web.enrollment.dto.InscricaoCursoResponse;
import com.fiap.feedbacks.application.catalog.ConsultarCursoUseCase;
import com.fiap.feedbacks.application.catalog.CriarAulaUseCase;
import com.fiap.feedbacks.application.catalog.CriarCursoUseCase;
import com.fiap.feedbacks.application.catalog.ListarAulasDoCursoUseCase;
import com.fiap.feedbacks.application.catalog.ListarCursosUseCase;
import com.fiap.feedbacks.application.enrollment.InscreverEmAulaUseCase;
import com.fiap.feedbacks.application.enrollment.InscreverEmCursoUseCase;
import com.fiap.feedbacks.domain.catalog.Aula;
import com.fiap.feedbacks.domain.catalog.Curso;
import com.fiap.feedbacks.domain.enrollment.InscricaoAula;
import com.fiap.feedbacks.domain.enrollment.InscricaoCurso;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/cursos")
@Produces(MediaType.APPLICATION_JSON)
public class CursoResource {

    @Inject
    CriarCursoUseCase criarCursoUseCase;
    @Inject
    ListarCursosUseCase listarCursosUseCase;
    @Inject
    ConsultarCursoUseCase consultarCursoUseCase;
    @Inject
    CriarAulaUseCase criarAulaUseCase;
    @Inject
    ListarAulasDoCursoUseCase listarAulasDoCursoUseCase;
    @Inject
    InscreverEmCursoUseCase inscreverEmCursoUseCase;
    @Inject
    InscreverEmAulaUseCase inscreverEmAulaUseCase;

    @GET
    @RolesAllowed({"ESTUDANTE", "ADMINISTRADOR"})
    public List<CursoResponse> listar() {
        return listarCursosUseCase.execute().stream()
                .map(this::toCursoResponse)
                .toList();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ESTUDANTE", "ADMINISTRADOR"})
    public CursoResponse consultar(@PathParam("id") UUID id) {
        return toCursoResponse(consultarCursoUseCase.execute(id));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public Response criar(@Valid CriarCursoRequest request) {
        var curso = criarCursoUseCase.execute(request.nome(), request.descricao());
        return Response.status(Response.Status.CREATED)
                .entity(toCursoResponse(curso))
                .build();
    }

    @POST
    @Path("/{cursoId}/aulas")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public Response criarAula(@PathParam("cursoId") UUID cursoId, @Valid CriarAulaRequest request) {
        var aula = criarAulaUseCase.execute(cursoId, request.nome(), request.descricao());
        return Response.status(Response.Status.CREATED)
                .entity(toAulaResponse(aula))
                .build();
    }

    @GET
    @Path("/{cursoId}/aulas")
    @RolesAllowed({"ESTUDANTE", "ADMINISTRADOR"})
    public List<AulaResponse> listarAulas(@PathParam("cursoId") UUID cursoId) {
        return listarAulasDoCursoUseCase.execute(cursoId).stream()
                .map(this::toAulaResponse)
                .toList();
    }

    @POST
    @Path("/{cursoId}/inscricoes")
    @RolesAllowed("ESTUDANTE")
    public Response inscrever(@PathParam("cursoId") UUID cursoId) {
        InscricaoCurso inscricao = inscreverEmCursoUseCase.execute(cursoId);
        return Response.status(Response.Status.CREATED)
                .entity(toInscricaoCursoResponse(inscricao))
                .build();
    }

    @POST
    @Path("/{cursoId}/aulas/{aulaId}/inscricoes")
    @RolesAllowed("ESTUDANTE")
    public Response inscreverEmAula(
            @PathParam("cursoId") UUID cursoId,
            @PathParam("aulaId") UUID aulaId
    ) {
        InscricaoAula inscricao = inscreverEmAulaUseCase.execute(cursoId, aulaId);
        return Response.status(Response.Status.CREATED)
                .entity(toInscricaoAulaResponse(inscricao))
                .build();
    }

    private CursoResponse toCursoResponse(Curso curso) {
        return new CursoResponse(curso.id(), curso.nome(), curso.descricao());
    }

    private AulaResponse toAulaResponse(Aula aula) {
        return new AulaResponse(aula.id(), aula.cursoId(), aula.nome(), aula.descricao());
    }

    private InscricaoCursoResponse toInscricaoCursoResponse(InscricaoCurso inscricao) {
        return new InscricaoCursoResponse(inscricao.id(), inscricao.cursoId(), inscricao.estudanteId());
    }

    private InscricaoAulaResponse toInscricaoAulaResponse(InscricaoAula inscricao) {
        return new InscricaoAulaResponse(
                inscricao.id(),
                inscricao.cursoId(),
                inscricao.aulaId(),
                inscricao.estudanteId()
        );
    }
}
