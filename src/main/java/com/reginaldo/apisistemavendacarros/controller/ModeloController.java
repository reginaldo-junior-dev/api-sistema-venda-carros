package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.modelo.ModeloRequest;
import com.reginaldo.apisistemavendacarros.dto.modelo.ModeloResponse;
import com.reginaldo.apisistemavendacarros.service.ModeloService;
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
@RequestMapping("/modelo")
@RequiredArgsConstructor
@Tag(name = "Modelos")
public class ModeloController {

    private final ModeloService modeloService;

    @PostMapping
    @Operation(summary = "[Admin] Cadastrar modelo")
    public ResponseEntity<ModeloResponse> cadastro (@Valid @RequestBody ModeloRequest request) {
        ModeloResponse modeloResponse = modeloService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(modeloResponse);
    }

    @GetMapping
    @Operation(summary = "Listar modelos")
    @SecurityRequirements
    public ResponseEntity<List<ModeloResponse>> lista() {
        List<ModeloResponse> modeloResponses = modeloService.listar();
        return ResponseEntity.ok(modeloResponses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar modelo por id")
    @SecurityRequirements
    public ResponseEntity<ModeloResponse> buscarPorId (@PathVariable UUID id) {
        ModeloResponse modeloResponse = modeloService.buscarPorId(id);
        return ResponseEntity.ok(modeloResponse);
    }

    @PutMapping("/{id}")
    @Operation(summary = "[Admin] Atualizar modelo")
    public ResponseEntity<ModeloResponse> atualizar (@PathVariable UUID id, @Valid @RequestBody ModeloRequest request) {
        ModeloResponse modeloResponse = modeloService.atualizar(id, request);
        return ResponseEntity.ok(modeloResponse);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "[Admin] Excluir modelo")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        modeloService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
