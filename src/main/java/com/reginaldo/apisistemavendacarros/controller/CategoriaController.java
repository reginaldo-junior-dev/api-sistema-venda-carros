package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.categoria.CategoriaRequest;
import com.reginaldo.apisistemavendacarros.dto.categoria.CategoriaResponse;
import com.reginaldo.apisistemavendacarros.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    @PostMapping
    @Operation(summary = "[Admin] Cadastrar categoria")
    public ResponseEntity<CategoriaResponse> cadastro (@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse categoriaResponse = categoriaService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaResponse);
    }

    @GetMapping
    @Operation(summary = "Listar categorias")
    @SecurityRequirements
    public ResponseEntity<List<CategoriaResponse>> lista() {
        List<CategoriaResponse> categoriaResponses = categoriaService.listar();
        return ResponseEntity.ok(categoriaResponses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar categoria por id")
    @SecurityRequirements
    public ResponseEntity<CategoriaResponse> buscarPorId (@PathVariable UUID id) {
        CategoriaResponse categoriaResponse = categoriaService.buscarPorId(id);
        return ResponseEntity.ok(categoriaResponse);
    }

    @PutMapping("/{id}")
    @Operation(summary = "[Admin] Atualizar categoria")
    public ResponseEntity<CategoriaResponse> atualizar (@PathVariable UUID id, @Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse categoriaResponse = categoriaService.atualizar(id, request);
        return ResponseEntity.ok(categoriaResponse);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "[Admin] Excluir categoria")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        categoriaService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
