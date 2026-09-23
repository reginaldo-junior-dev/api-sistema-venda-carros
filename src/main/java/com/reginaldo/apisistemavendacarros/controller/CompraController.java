package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.CompraRequest;
import com.reginaldo.apisistemavendacarros.dto.CompraResponse;
import com.reginaldo.apisistemavendacarros.service.CompraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/compra")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;

    @PostMapping
    public ResponseEntity<CompraResponse> cadastro (@RequestBody CompraRequest request) {
        CompraResponse compraResponse = compraService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(compraResponse);
    }

    @GetMapping
    public ResponseEntity<List<CompraResponse>> lista() {
        List<CompraResponse> compraResponses = compraService.listar();
        return ResponseEntity.ok(compraResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompraResponse> buscarPorId (@PathVariable UUID id) {
        CompraResponse compraResponse = compraService.buscarPorId(id);
        return ResponseEntity.ok(compraResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompraResponse> atualizar (@PathVariable UUID id, @RequestBody CompraRequest request) {
        CompraResponse compraResponse = compraService.atualizar(id, request);
        return ResponseEntity.ok(compraResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        compraService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
