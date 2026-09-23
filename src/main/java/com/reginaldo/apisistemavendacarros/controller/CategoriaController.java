package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.CategoriaRequest;
import com.reginaldo.apisistemavendacarros.dto.CategoriaResponse;
import com.reginaldo.apisistemavendacarros.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categoria")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @PostMapping
    public ResponseEntity<CategoriaResponse> cadastro (@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse categoriaResponse = categoriaService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaResponse);
    }

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> lista() {
        List<CategoriaResponse> categoriaResponses = categoriaService.listar();
        return ResponseEntity.ok(categoriaResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> buscarPorId (@PathVariable UUID id) {
        CategoriaResponse categoriaResponse = categoriaService.buscarPorId(id);
        return ResponseEntity.ok(categoriaResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> atualizar (@PathVariable UUID id, @Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse categoriaResponse = categoriaService.atualizar(id, request);
        return ResponseEntity.ok(categoriaResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        categoriaService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
