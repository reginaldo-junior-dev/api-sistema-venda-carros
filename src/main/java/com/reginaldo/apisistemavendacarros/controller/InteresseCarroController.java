package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.InteresseCarroRequest;
import com.reginaldo.apisistemavendacarros.dto.InteresseCarroResponse;
import com.reginaldo.apisistemavendacarros.service.InteresseCarroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/interesse-carro")
@RequiredArgsConstructor
public class InteresseCarroController {

    private final InteresseCarroService interesseCarroService;

    @PostMapping
    public ResponseEntity<InteresseCarroResponse> cadastro (@Valid @RequestBody InteresseCarroRequest request) {
        InteresseCarroResponse interesseCarroResponse = interesseCarroService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(interesseCarroResponse);
    }

    @GetMapping
    public ResponseEntity<List<InteresseCarroResponse>> lista() {
        List<InteresseCarroResponse> interesseCarroResponses = interesseCarroService.listar();
        return ResponseEntity.ok(interesseCarroResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InteresseCarroResponse> buscarPorId (@PathVariable UUID id) {
        InteresseCarroResponse interesseCarroResponse = interesseCarroService.buscarPorId(id);
        return ResponseEntity.ok(interesseCarroResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InteresseCarroResponse> atualizar (@PathVariable UUID id, @Valid @RequestBody InteresseCarroRequest request) {
        InteresseCarroResponse interesseCarroResponse = interesseCarroService.atualizar(id, request);
        return ResponseEntity.ok(interesseCarroResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        interesseCarroService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
