package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.CorRequest;
import com.reginaldo.apisistemavendacarros.dto.CorResponse;
import com.reginaldo.apisistemavendacarros.service.CorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cor")
@RequiredArgsConstructor
public class CorController {

    private final CorService corService;

    @PostMapping
    public ResponseEntity<CorResponse> cadastro (@Valid @RequestBody CorRequest request) {
        CorResponse corResponse = corService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(corResponse);
    }

    @GetMapping
    public ResponseEntity<List<CorResponse>> lista () {
        List<CorResponse> corResponses = corService.listar();
        return ResponseEntity.ok(corResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CorResponse> buscarPorId (@PathVariable UUID id) {
        CorResponse corResponse = corService.buscarPorId(id);
        return ResponseEntity.ok(corResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CorResponse> atualizar (@PathVariable UUID id, @Valid @RequestBody CorRequest request) {
        CorResponse corResponse = corService.atualizar(id, request);
        return ResponseEntity.ok(corResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        corService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
