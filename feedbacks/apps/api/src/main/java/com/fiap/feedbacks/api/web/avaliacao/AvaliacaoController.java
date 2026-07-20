package com.fiap.feedbacks.api.web.avaliacao;

import com.fiap.feedbacks.application.avaliacao.ListarAvaliacoesUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/avaliacoes")
public class AvaliacaoController {

    private final ListarAvaliacoesUseCase listarAvaliacoesUseCase;

    public AvaliacaoController(ListarAvaliacoesUseCase listarAvaliacoesUseCase) {
        this.listarAvaliacoesUseCase = listarAvaliacoesUseCase;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar() {
        return ResponseEntity.ok(listarAvaliacoesUseCase.execute());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> criar(@RequestBody Map<String, String> request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "id", UUID.randomUUID(),
                        "descricao", request.getOrDefault("descricao", "Feedback demo")
                ));
    }
}
