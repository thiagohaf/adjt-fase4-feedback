package com.fiap.feedbacks.api.web.catalog;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/aulas")
public class AulaController {

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> consultar(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("id", id, "titulo", "Aula Demo"));
    }
}
