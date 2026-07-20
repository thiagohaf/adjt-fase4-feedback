package com.fiap.feedbacks.api.web.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cursos")
public class CursoController {

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar() {
        return ResponseEntity.ok(List.of(Map.of("id", UUID.randomUUID(), "nome", "Curso Demo")));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> criar(@RequestBody Map<String, String> request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", UUID.randomUUID(), "nome", request.getOrDefault("nome", "Novo Curso")));
    }

    @PostMapping("/{cursoId}/aulas")
    public ResponseEntity<Map<String, Object>> criarAula(
            @PathVariable UUID cursoId,
            @RequestBody Map<String, String> request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "id", UUID.randomUUID(),
                        "cursoId", cursoId,
                        "titulo", request.getOrDefault("titulo", "Nova Aula")
                ));
    }

    @GetMapping("/{cursoId}/aulas")
    public ResponseEntity<List<Map<String, Object>>> listarAulas(@PathVariable UUID cursoId) {
        return ResponseEntity.ok(List.of(Map.of(
                "id", UUID.randomUUID(),
                "cursoId", cursoId,
                "titulo", "Aula Demo"
        )));
    }

    @PostMapping("/{cursoId}/inscricoes")
    public ResponseEntity<Map<String, Object>> inscrever(@PathVariable UUID cursoId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("cursoId", cursoId, "status", "INSCRITO"));
    }
}
