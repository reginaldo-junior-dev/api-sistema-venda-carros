package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.ModeloRequest;
import com.reginaldo.apisistemavendacarros.dto.ModeloResponse;
import com.reginaldo.apisistemavendacarros.service.ModeloService;
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
public class ModeloController {

    private final ModeloService modeloService;

    @PostMapping
    public ResponseEntity<ModeloResponse> cadastro (@Valid @RequestBody ModeloRequest request) {
        ModeloResponse modeloResponse = modeloService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(modeloResponse);
    }

    @GetMapping
    public ResponseEntity<List<ModeloResponse>> lista() {
        List<ModeloResponse> modeloResponses = modeloService.listar();
        return ResponseEntity.ok(modeloResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModeloResponse> buscarPorId (@PathVariable UUID id) {
        ModeloResponse modeloResponse = modeloService.buscarPorId(id);
        return ResponseEntity.ok(modeloResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModeloResponse> atualizar (@PathVariable UUID id, @Valid @RequestBody ModeloRequest request) {
        ModeloResponse modeloResponse = modeloService.atualizar(id, request);
        return ResponseEntity.ok(modeloResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        modeloService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
