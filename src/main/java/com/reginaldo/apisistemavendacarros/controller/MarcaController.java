package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.MarcaRequest;
import com.reginaldo.apisistemavendacarros.dto.MarcaResponse;
import com.reginaldo.apisistemavendacarros.service.MarcaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/marca")
@RequiredArgsConstructor
public class MarcaController {

    private final MarcaService marcaService;

    @PostMapping
    public ResponseEntity<MarcaResponse> cadastro (@RequestBody MarcaRequest request) {
        MarcaResponse marcaResponse = marcaService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(marcaResponse);
    }

    @GetMapping
    public ResponseEntity<List<MarcaResponse>> lista () {
        List<MarcaResponse> marcaResponses = marcaService.lista();
        return ResponseEntity.ok(marcaResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarcaResponse> buscarPorId (@PathVariable UUID id) {
        MarcaResponse marcaResponse = marcaService.buscarPorId(id);
        return ResponseEntity.ok(marcaResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MarcaResponse> atualizar (@PathVariable UUID id, @RequestBody MarcaRequest request) {
        MarcaResponse marcaResponse = marcaService.atualizar(id, request);
        return ResponseEntity.ok(marcaResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        marcaService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
